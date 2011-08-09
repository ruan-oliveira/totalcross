/*********************************************************************************
 *  TotalCross Software Development Kit - Litebase                               *
 *  Copyright (C) 2000-2011 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *********************************************************************************/

/**
 * Declares functions for a normal file, ie, a file that is stored on disk.
 */

#ifndef LITEBASE_NORMAL_FILE_H
#define LITEBASE_NORMAL_FILE_H

#include "Litebase.h"

/**
 * Creates a disk file to store tables.
 *
 * @param context The thread context where the function is being executed.
 * @param name The name of the file.
 * @param isCreation Indicates if the file must be created or just open.
 * @param sourcePath The path where the file will be created.
 * @param slot The slot being used on palm or -1 for the other devices.
 * @param xFile A pointer to the normal file structure.
 * @param cacheSize The cache size to be used. -1 should be passed if the default value is to be used.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 * @throws DriverException If the file cannot be open.
 * @throws OutOfMemoryError If there is not enough memory to create the normal file cache.
 */
bool nfCreateFile(Context context, CharP name, bool isCreation, CharP sourcePath, int32 slot, XFile* xFile, int32 cacheSize);

/**
 * Reads file bytes.
 *
 * @param context The thread context where the function is being executed.
 * @param xFile A pointer to the normal file structure.
 * @param buffer The byte array to read data into.
 * @param count The number of bytes to read.
 * @return The number of bytes read or -1 if an error occurred or the end of file was achieved.
 */
int32 nfReadBytes(Context context, XFile* xFile, uint8* buffer, int32 count);

/**
 * Write bytes in a file.
 *
 * @param context The thread context where the function is being executed.
 * @param xFile A pointer to the normal file structure.
 * @param buffer The byte array to write data from.
 * @param count The number of bytes to write.
 * @return The number of bytes written or -1 if an error occurred or the end of file was achieved.
 */
int32 nfWriteBytes(Context context, XFile* xFile, uint8* buffer, int32 count);

/**
 * Enlarges the file. This function MUST be called to grow the file.
 *
 * @param context The thread context where the function is being executed.
 * @param xFile A pointer to the normal file structure.
 * @param newSize The new size for the file.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 * @throws DriverException If it is not possible to grow the file.
 */
bool nfGrowTo(Context context, XFile* xFile, uint32 newSize);

/**
 * Sets the current file position.
 *
 * @param xFile A pointer to the normal file structure.
 * @param newPos The new file position. 
 */
void nfSetPos(XFile* xFile, int32 newPos);

/**
 * Renames a file
 *
 * @param context The thread context where the function is being executed.
 * @param xFile A pointer to the normal file structure.
 * @param newName The new name of the file.
 * @param sourcePath The path where the file is stored.
 * @param slot The slot being used on palm or -1 for the other devices.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 * @throws DriverException If it is not possible to rename the file.
 */
bool nfRename(Context context, XFile* xFile, CharP newName, CharP sourcePath, int32 slot);

/** 
 * Closes a file.
 * 
 * @param context The thread context where the function is being executed.
 * @param xFile A pointer to the normal file structure.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 * @throws DriverException If it is not possible to close the file.
 */
bool nfClose(Context context, XFile* xFile);

/** 
 * Removes a file.
 * 
 * @param context The thread context where the function is being executed.
 * @param xFile A pointer to the normal file structure.
 * @param sourcePath The path where the file is stored.
 * @param slot The slot being used on palm or -1 for the other devices.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 * @throws DriverException If it is not possible to remove the file.
 */
bool nfRemove(Context context, XFile* xFile, CharP sourcePath, int32 slot);

/**
 * The cache must be refreshed if what is desired is not inside it.
 *
 * @param context The thread context where the function is being executed.
 * @param xFile A pointer to the normal file structure.
 * @param count The number of bytes that must be read.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 * @throws DriverException If it is not possible to read from the file.
 * @throws OutOfMemoryError If there is not enough memory to enlarge the normal file cache.
 */
bool refreshCache(Context context, XFile* xFile, int32 count);

/**
 * Flushs the cache into the disk.
 *
 * @param context The thread context where the function is being executed.
 * @param xFile A pointer to the normal file structure.
 * @return <code>false</code> if an error occurs; <code>true</code>, otherwise.
 * @throws DriverException If it is not possible to write to the file.
 */
bool flushCache(Context context, XFile* xFile);

/**
 * Prepares an error message when an error occurs when dealing with files.
 * 
 * @param context The thread context where the function is being executed.
 * @param errorCode The file error code.
 * @param fileName The file where the error ocurred.
 * @throws DriverException An exception with the error message.
 */
void fileError(Context context, int32 errorCode, CharP fileName);

#endif
