/*
 *  Copyright(C) 2007 Cameron Rich
 *
 *  This library is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation; either version 2.1 of the License, or
 *  (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Lesser License
 *  along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * Enable a subset of openssl compatible functions. We don't aim to be 100%
 * compatible - just to be able to do basic ports etc.
 *
 * Only really tested on mini_httpd, so I'm not too sure how extensive this
 * port is.
 */

#include "axssl_config.h"

#ifdef CONFIG_OPENSSL_COMPATIBLE
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include "ssl.h"

#define OPENSSL_CTX_ATTR  ((OPENSSL_CTX *)ssl_ctx->bonus_attr)

typedef struct _SSL_METHOD SSL_METHOD;

SSL_METHOD *SSLv23_server_method(void) { return (SSL_METHOD*)1; }
SSL_METHOD *SSLv3_server_method(void)  { return (SSL_METHOD*)2; }
SSL_METHOD *TLSv1_server_method(void)  { return (SSL_METHOD*)3; }
SSL_METHOD *SSLv23_client_method(void) { return (SSL_METHOD*)10; }
SSL_METHOD *SSLv3_client_method(void)  { return (SSL_METHOD*)11; }
SSL_METHOD *TLSv1_client_method(void)  { return (SSL_METHOD*)12; }

typedef void * (*bio_func_type_t)(void);

typedef struct
{
   SSL_METHOD *ssl_methods;
   char *password;
} OPENSSL_CTX;

typedef int pem_password_cb(char *buf, int size, int rwflag, void *userdata);

void SSL_CTX_set_default_passwd_cb(SSL_CTX *ssl_ctx, pem_password_cb *cb)
{
   char buffer[128];
   int size;

   if (OPENSSL_CTX_ATTR->password)
      free(OPENSSL_CTX_ATTR->password);

   size = cb(buffer, sizeof(buffer), 0, NULL);
   if (size > 0)
   {
      buffer[size] = '\0';
#if defined(WINCE) && _WIN32_WCE >= 300
      OPENSSL_CTX_ATTR->password = strdup(buffer);
#else
      OPENSSL_CTX_ATTR->password = malloc(strlen(buffer));
      if (OPENSSL_CTX_ATTR->password)
         memmove(OPENSSL_CTX_ATTR->password, buffer, strlen(buffer)+1);
#endif
   }
}

SSL_CTX * SSL_CTX_new(SSL_METHOD *meth)
{
   SSL_CTX *ssl_ctx = ssl_ctx_new(0, 5);
   ssl_ctx->bonus_attr = malloc(sizeof(OPENSSL_CTX));
   OPENSSL_CTX_ATTR->ssl_methods = meth;
   OPENSSL_CTX_ATTR->password = NULL;
   return ssl_ctx;
}

void SSL_CTX_free(SSL_CTX * ssl_ctx)
{
   if (OPENSSL_CTX_ATTR->password)
      free(OPENSSL_CTX_ATTR->password);
   free(ssl_ctx->bonus_attr);
   ssl_ctx_free(ssl_ctx);
}

SSL * SSL_new(SSL_CTX *ssl_ctx)
{
    SSL *ssl;
    //ssl_func_type_t ssl_func_type;
    SSL_METHOD *ssl_methods;

    ssl = ssl_new(ssl_ctx, -1);        /* fd is set later */
    //ssl_func_type = OPENSSL_CTX_ATTR->ssl_func_type;
    ssl_methods = OPENSSL_CTX_ATTR->ssl_methods;

#ifdef CONFIG_SSL_ENABLE_CLIENT
    //if (ssl_func_type == SSLv23_client_method ||
    //    ssl_func_type == SSLv3_client_method ||
    //    ssl_func_type == TLSv1_client_method)
    if (ssl_methods >= SSLv23_client_method())
    {
        SET_SSL_FLAG(SSL_IS_CLIENT);
    }
    else
#endif
    {
        ssl->next_state = HS_CLIENT_HELLO;
    }

    return ssl;
}

int SSL_set_fd(SSL *s, int fd)
{
    s->client_fd = fd;
    return 1;   /* always succeeds */
}

int SSL_accept(SSL *ssl)
{
    while (ssl_read(ssl, NULL) == SSL_OK)
    {
        if (ssl->next_state == HS_CLIENT_HELLO)
            return 1;   /* we're done */
    }

    return -1;
}

#ifdef CONFIG_SSL_ENABLE_CLIENT
int SSL_connect(SSL *ssl)
{
    return do_client_connect(ssl) == SSL_OK ? 1 : -1;
}
#endif

void SSL_free(SSL *ssl)
{
    ssl_free(ssl);
}

int SSL_read(SSL *ssl, void *buf, int num)
{
    uint8_t *read_buf;
    int ret;

    while ((ret = ssl_read(ssl, &read_buf)) == SSL_OK);

    if (ret > SSL_OK)
    {
        memcpy(buf, read_buf, ret > num ? num : ret);
    }

    return ret;
}

int SSL_write(SSL *ssl, const void *buf, int num)
{
    return ssl_write(ssl, buf, num);
}

int SSL_CTX_use_certificate_file(SSL_CTX *ssl_ctx, const char *file, int type)
{
    return (ssl_obj_load(ssl_ctx, SSL_OBJ_X509_CERT, file, OPENSSL_CTX_ATTR->password) == SSL_OK);
}

int SSL_CTX_use_RSAPrivateKey_file(SSL_CTX *ssl_ctx, const char *file, int type)
{
    return (ssl_obj_load(ssl_ctx, SSL_OBJ_RSA_KEY, file, OPENSSL_CTX_ATTR->password) == SSL_OK);
}

int SSL_CTX_use_PrivateKey_file(SSL_CTX *ssl_ctx, const char *file, int type)
{
    return (ssl_obj_load(ssl_ctx, SSL_OBJ_RSA_KEY, file, OPENSSL_CTX_ATTR->password) == SSL_OK);
}

int SSL_CTX_use_certificate_ASN1(SSL_CTX *ssl_ctx, int len, const uint8_t *d)
{
    return (ssl_obj_memory_load(ssl_ctx, SSL_OBJ_X509_CERT, d, len, OPENSSL_CTX_ATTR->password) == SSL_OK);
}

int SSL_CTX_load_verify_locations(SSL_CTX *ssl_ctx, const char *CAfile, const char *CApath)
{
   char fn[256];
   strcpy(fn, CApath ? CApath : "");
   if (CApath)
      strcat(fn, "/");
   strcat(fn, CAfile);
   return (ssl_obj_load(ssl_ctx, SSL_OBJ_X509_CACERT, fn, OPENSSL_CTX_ATTR->password) == SSL_OK);
}

#if 0
const uint8_t *SSL_get_session(const SSL *ssl)
{
    /* TODO: return SSL_SESSION type */
    return ssl_get_session_id(ssl);
}
#endif

int SSL_CTX_check_private_key(const SSL_CTX *ctx)
{
    return 1;
}

int SSL_CTX_set_cipher_list(SSL *s, const char *str)
{
    return 1;
}

int SSL_get_error(const SSL *ssl, int ret)
{
    ssl_display_error(ret);
    return 0;   /* TODO: return proper return code */
}

void SSL_CTX_set_options(SSL_CTX *ssl_ctx, int option) {}
int SSL_library_init() { return 1; }
void SSL_load_error_strings() {}
void ERR_print_errors_fp(FILE *fp) {}
long SSL_CTX_get_timeout(const SSL_CTX *ssl_ctx) {
                            return CONFIG_SSL_EXPIRY_TIME*3600; }
long SSL_CTX_set_timeout(SSL_CTX *ssl_ctx, long t) {
                            return SSL_CTX_get_timeout(ssl_ctx); }
void BIO_printf(FILE *f, const char *format, ...)
{
    va_list(ap);
    va_start(ap, format);
    vfprintf(f, format, ap);
    va_end(ap);
}

void* BIO_s_null(void) { return NULL; }
FILE *BIO_new(bio_func_type_t func)
{
    if (func == BIO_s_null)
        return fopen("/dev/null", "r");
    else
        return NULL;
}

FILE *BIO_new_fp(FILE *stream, int close_flag) { return stream; }
int BIO_free(FILE *a) { if (a != stdout && a != stderr) fclose(a); return 1; }



#endif