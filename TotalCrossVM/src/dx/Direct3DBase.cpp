﻿#include "Direct3DBase.h"
#include <thread>
#include <mutex>
#include "PhoneDirect3DXamlAppComponent.h"
#define HAS_TCHAR
#include "tcvm.h"

using namespace DirectX;
using namespace Microsoft::WRL;
using namespace Windows::Foundation;
using namespace Windows::UI::Core;

#define DXRELEASE(x) do {if (x) {x->Release(); x = null;}} while (0)
static Direct3DBase ^instance;

extern "C" 
{ 
   extern int32 appW, appH, glShiftY; 
   void recreateTextures();
   void repaintActiveWindows(Context currentContext);
}

// Constructor.
Direct3DBase::Direct3DBase(PhoneDirect3DXamlAppComponent::CSwrapper ^cs)
{
   csharp = cs;
   instance = this;
}

Direct3DBase ^Direct3DBase::getLastInstance()
{
	return instance;
}

// Initialize the Direct3D resources required to run.
void Direct3DBase::initialize(bool resuming)
{
   // create the D3DDevice
   UINT creationFlags = D3D11_CREATE_DEVICE_BGRA_SUPPORT;
   //creationFlags |= D3D11_CREATE_DEVICE_DEBUG;
   D3D_FEATURE_LEVEL featureLevels[] = {D3D_FEATURE_LEVEL_9_3};
   DX::ThrowIfFailed(D3D11CreateDevice(nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr, creationFlags, featureLevels, ARRAYSIZE(featureLevels), D3D11_SDK_VERSION, &d3dDevice, &m_featureLevel, &d3dImedContext));
   d3dDevice->CreateDeferredContext(0, &d3dcontext);

   loadCompleted = 0;
   updateWS = true;
   if (!resuming)
   {
      wchar_t mensagem_fim[2048];
      int saida;

      saida = startVM("UIControls", &localContext);

      if (saida != 0)
      {
         swprintf_s(mensagem_fim, 1000, L"Error code in starting VM: %d", saida);
         csharp->privateAlertCS(ref new Platform::String(mensagem_fim));
      }
   }

   auto loadVSTask1 = DX::ReadDataAsync("VertexShaderGlobalColor.cso");
   auto loadPSTask1 = DX::ReadDataAsync("PixelShaderGlobalColor.cso");

   auto loadVSTask2 = DX::ReadDataAsync("VertexShaderTexture.cso");
   auto loadPSTask2 = DX::ReadDataAsync("PixelShaderTexture.cso");

   auto loadVSTask3 = DX::ReadDataAsync("VertexShaderLocalColor.cso");
   auto loadPSTask3 = DX::ReadDataAsync("PixelShaderLocalColor.cso");

   // global color vertex
   auto createVSTask1 = loadVSTask1.then([this](Platform::Array<byte>^ fileData) 
   {
      DXRELEASE(vertexShader);
      DXRELEASE(inputLayout);
      DX::ThrowIfFailed(d3dDevice->CreateVertexShader(fileData->Data, fileData->Length, nullptr, &vertexShader));
      const D3D11_INPUT_ELEMENT_DESC vertexDesc[] =
      {
         { "POSITION", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
      };
      DX::ThrowIfFailed(d3dDevice->CreateInputLayout(vertexDesc, ARRAYSIZE(vertexDesc), fileData->Data, fileData->Length, &inputLayout));
      loadCompleted |= 1;
   });

   // global color pixel
   auto createPSTask1 = loadPSTask1.then([this](Platform::Array<byte>^ fileData)
   {
      DXRELEASE(pixelShader);
      DXRELEASE(constantBuffer);
      DX::ThrowIfFailed(d3dDevice->CreatePixelShader(fileData->Data, fileData->Length, nullptr, &pixelShader));
      CD3D11_BUFFER_DESC constantBufferDesc(sizeof(ProjectionConstantBuffer), D3D11_BIND_CONSTANT_BUFFER);
      DX::ThrowIfFailed(d3dDevice->CreateBuffer(&constantBufferDesc, nullptr, &constantBuffer));
      loadCompleted |= 2;
   });

   // texture vertex
   auto createVSTask2 = loadVSTask2.then([this](Platform::Array<byte>^ fileData) 
   {
      DXRELEASE(vertexShaderT);
      DXRELEASE(inputLayoutT);
      DX::ThrowIfFailed(d3dDevice->CreateVertexShader(fileData->Data, fileData->Length, nullptr, &vertexShaderT));
      const D3D11_INPUT_ELEMENT_DESC vertexDesc[] =
      {
         { "POSITION", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
         { "TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 8, D3D11_INPUT_PER_VERTEX_DATA, 0 },
      };
      DX::ThrowIfFailed(d3dDevice->CreateInputLayout(vertexDesc, ARRAYSIZE(vertexDesc), fileData->Data, fileData->Length, &inputLayoutT));
      loadCompleted |= 4;
   });

   // texture pixel
   auto createPSTask2 = loadPSTask2.then([this](Platform::Array<byte>^ fileData)
   {
      DXRELEASE(pixelShaderT);
      DX::ThrowIfFailed(d3dDevice->CreatePixelShader(fileData->Data, fileData->Length, nullptr, &pixelShaderT));
      loadCompleted |= 8;
   });

   // local color vertex
   auto createVSTask3 = loadVSTask3.then([this](Platform::Array<byte>^ fileData)
   {
      DXRELEASE(vertexShaderLC);
      DXRELEASE(inputLayoutLC);
      DX::ThrowIfFailed(d3dDevice->CreateVertexShader(fileData->Data, fileData->Length, nullptr, &vertexShaderLC));
      const D3D11_INPUT_ELEMENT_DESC vertexDesc[] =
      {
         { "POSITION", 0, DXGI_FORMAT_R32G32_FLOAT, 0, 0, D3D11_INPUT_PER_VERTEX_DATA, 0 },
         { "COLOR", 0,    DXGI_FORMAT_R32G32B32A32_FLOAT, 0, 8, D3D11_INPUT_PER_VERTEX_DATA, 0 },
      };
      DX::ThrowIfFailed(d3dDevice->CreateInputLayout(vertexDesc, ARRAYSIZE(vertexDesc), fileData->Data, fileData->Length, &inputLayoutLC));
      loadCompleted |= 16;
   });

   // local color pixel
   auto createPSTask3 = loadPSTask3.then([this](Platform::Array<byte>^ fileData)
   {
      DXRELEASE(pixelShaderLC);
      DX::ThrowIfFailed(d3dDevice->CreatePixelShader(fileData->Data, fileData->Length, nullptr, &pixelShaderLC));
      loadCompleted |= 32;
   });
}

void Direct3DBase::updateDevice(IDrawingSurfaceRuntimeHostNative* host)
{
   updateWS |= rotatedTo == -2 || rotatedTo >= 0;
   if (!updateWS)
      return;

   DXRELEASE(depthStencil);
   DXRELEASE(depthStencilView);
   DXRELEASE(indexBuffer);
   DXRELEASE(pBufferColor);
   DXRELEASE(pBufferRect);
   DXRELEASE(pBufferRectLC);
   DXRELEASE(texsampler);
   DXRELEASE(depthDisabledStencilState);
   DXRELEASE(pBlendState);
   DXRELEASE(pRasterStateDisableClipping);
   DXRELEASE(pRasterStateEnableClipping);
   DXRELEASE(texVertexBuffer);
   DXRELEASE(renderTexView);
   DXRELEASE(renderTex);

   // Create a descriptor for the render target buffer.
   CD3D11_TEXTURE2D_DESC renderTargetDesc(DXGI_FORMAT_B8G8R8A8_UNORM, appW, appH, 1, 1, D3D11_BIND_RENDER_TARGET | D3D11_BIND_SHADER_RESOURCE);
   renderTargetDesc.MiscFlags = D3D11_RESOURCE_MISC_SHARED_KEYEDMUTEX | D3D11_RESOURCE_MISC_SHARED_NTHANDLE;

   // Allocate a 2-D surface as the render target buffer.
   DX::ThrowIfFailed(d3dDevice->CreateTexture2D(&renderTargetDesc, nullptr, &renderTex));
   DX::ThrowIfFailed(d3dDevice->CreateRenderTargetView(renderTex, nullptr, &renderTexView));
   host->CreateSynchronizedTexture(renderTex, &syncTex);

   // Create a depth stencil view.
   CD3D11_TEXTURE2D_DESC depthStencilDesc(DXGI_FORMAT_D24_UNORM_S8_UINT, appW, appH, 1, 1, D3D11_BIND_DEPTH_STENCIL);
   DX::ThrowIfFailed(d3dDevice->CreateTexture2D(&depthStencilDesc, nullptr, &depthStencil));
   CD3D11_DEPTH_STENCIL_VIEW_DESC depthStencilViewDesc(D3D11_DSV_DIMENSION_TEXTURE2D);
   DX::ThrowIfFailed(d3dDevice->CreateDepthStencilView(depthStencil, &depthStencilViewDesc, &depthStencilView));

   XMMATRIX mat = rotatedTo == 0 || rotatedTo == -2 ?
      XMMatrixOrthographicOffCenterLH(0, (float)appW, (float)appH, 0, -1.0f, 1.0f) :
      XMMatrixMultiply(XMMatrixRotationX(XM_PIDIV2), XMMatrixOrthographicOffCenterLH(0, (float)appW, (float)appH, 0, -1.0f, 1.0f));
   XMStoreFloat4x4(&constantBufferData.projection, mat);

   unsigned short cubeIndices[] =
   {
      0, 1, 2, 0, 2, 3
   };
   D3D11_SUBRESOURCE_DATA indexBufferData = { 0 };
   indexBufferData.pSysMem = cubeIndices;
   CD3D11_BUFFER_DESC indexBufferDesc(sizeof(cubeIndices), D3D11_BIND_INDEX_BUFFER);
   DX::ThrowIfFailed(d3dDevice->CreateBuffer(&indexBufferDesc, &indexBufferData, &indexBuffer));

   // used in setColor for fillRect and drawLine and also textures
   {
      D3D11_BUFFER_DESC bd = { 0 };
      bd.Usage = D3D11_USAGE_DYNAMIC;                // write access access by CPU and GPU
      bd.ByteWidth = sizeof(VertexColor);             // size is the VERTEX struct * 3
      bd.BindFlags = D3D11_BIND_CONSTANT_BUFFER;       // use as a vertex buffer
      bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;    // allow CPU to write in buffer
      DX::ThrowIfFailed(d3dDevice->CreateBuffer(&bd, NULL, &pBufferColor));       // create the buffer
   }
   // used in fillRect and drawLine
   {
      D3D11_BUFFER_DESC bd = { 0 };
      bd.Usage = D3D11_USAGE_DYNAMIC;                // write access access by CPU and GPU
      bd.ByteWidth = sizeof(VertexPosition)* 4;     // size is the VERTEX
      bd.BindFlags = D3D11_BIND_VERTEX_BUFFER;       // use as a vertex buffer
      bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;    // allow CPU to write in buffer
      DX::ThrowIfFailed(d3dDevice->CreateBuffer(&bd, NULL, &pBufferRect));       // create the buffer
   }
   // used in fillShadedRect
   {
      D3D11_BUFFER_DESC bd = { 0 };
      bd.Usage = D3D11_USAGE_DYNAMIC;                // write access access by CPU and GPU
      bd.ByteWidth = sizeof(VertexPositionColor)* 4;             // size is the VERTEX
      bd.BindFlags = D3D11_BIND_VERTEX_BUFFER;       // use as a vertex buffer
      bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;    // allow CPU to write in buffer
      DX::ThrowIfFailed(d3dDevice->CreateBuffer(&bd, NULL, &pBufferRectLC));       // create the buffer
   }

   /////////// TEXTURE
   // Once the texture view is created, create a sampler.  This defines how the color
   // for a particular texture coordinate is determined using the relevant texture data.
   D3D11_SAMPLER_DESC samplerDesc;
   ZeroMemory(&samplerDesc, sizeof(samplerDesc));
   samplerDesc.Filter = D3D11_FILTER_MIN_MAG_MIP_LINEAR;
   samplerDesc.AddressU = samplerDesc.AddressV = samplerDesc.AddressW = D3D11_TEXTURE_ADDRESS_CLAMP; // Feature level 9_3, the display device supports the use of 2-D textures with dimensions that are not powers of two under two conditions. First, only one MIP-map level for each texture can be created, and second, no wrap sampler modes for textures are allowed (that is, the AddressU, AddressV, and AddressW members of D3D11_SAMPLER_DESC cannot be set to D3D11_TEXTURE_ADDRESS_WRAP).
   samplerDesc.MaxLOD = D3D11_FLOAT32_MAX;
   samplerDesc.ComparisonFunc = D3D11_COMPARISON_NEVER;
   DX::ThrowIfFailed(d3dDevice->CreateSamplerState(&samplerDesc, &texsampler));

   D3D11_DEPTH_STENCIL_DESC depthDisabledStencilDesc;
   depthDisabledStencilDesc.DepthEnable = false;
   depthDisabledStencilDesc.DepthWriteMask = D3D11_DEPTH_WRITE_MASK_ALL;
   depthDisabledStencilDesc.DepthFunc = D3D11_COMPARISON_LESS;
   depthDisabledStencilDesc.StencilEnable = true;
   depthDisabledStencilDesc.StencilReadMask = 0xFF;
   depthDisabledStencilDesc.StencilWriteMask = 0xFF;
   depthDisabledStencilDesc.FrontFace.StencilFailOp = D3D11_STENCIL_OP_KEEP;
   depthDisabledStencilDesc.FrontFace.StencilDepthFailOp = D3D11_STENCIL_OP_INCR;
   depthDisabledStencilDesc.FrontFace.StencilPassOp = D3D11_STENCIL_OP_KEEP;
   depthDisabledStencilDesc.FrontFace.StencilFunc = D3D11_COMPARISON_ALWAYS;
   depthDisabledStencilDesc.BackFace.StencilFailOp = D3D11_STENCIL_OP_KEEP;
   depthDisabledStencilDesc.BackFace.StencilDepthFailOp = D3D11_STENCIL_OP_DECR;
   depthDisabledStencilDesc.BackFace.StencilPassOp = D3D11_STENCIL_OP_KEEP;
   depthDisabledStencilDesc.BackFace.StencilFunc = D3D11_COMPARISON_ALWAYS;
   // Create the state using the device.
   DX::ThrowIfFailed(d3dDevice->CreateDepthStencilState(&depthDisabledStencilDesc, &depthDisabledStencilState));

   // setup alpha blending
   D3D11_BLEND_DESC blendStateDescription = { 0 };
   blendStateDescription.RenderTarget[0].BlendEnable = TRUE;
   blendStateDescription.RenderTarget[0].SrcBlend = D3D11_BLEND_SRC_ALPHA;
   blendStateDescription.RenderTarget[0].DestBlend = D3D11_BLEND_INV_SRC_ALPHA;
   blendStateDescription.RenderTarget[0].BlendOp = D3D11_BLEND_OP_ADD;
   blendStateDescription.RenderTarget[0].SrcBlendAlpha = D3D11_BLEND_ONE;
   blendStateDescription.RenderTarget[0].DestBlendAlpha = D3D11_BLEND_ZERO;
   blendStateDescription.RenderTarget[0].BlendOpAlpha = D3D11_BLEND_OP_ADD;
   blendStateDescription.RenderTarget[0].RenderTargetWriteMask = 0x0f;
   DX::ThrowIfFailed(d3dDevice->CreateBlendState(&blendStateDescription, &pBlendState));

   // setup clipping
   D3D11_RASTERIZER_DESC rasterizerState = { D3D11_FILL_SOLID };
   rasterizerState.CullMode = D3D11_CULL_FRONT;
   rasterizerState.FrontCounterClockwise = true;
   rasterizerState.DepthClipEnable = true;
   DX::ThrowIfFailed(d3dDevice->CreateRasterizerState(&rasterizerState, &pRasterStateDisableClipping));
   rasterizerState.ScissorEnable = true;
   DX::ThrowIfFailed(d3dDevice->CreateRasterizerState(&rasterizerState, &pRasterStateEnableClipping));

   // texture vertices
   D3D11_BUFFER_DESC bd = { 0 };
   bd.Usage = D3D11_USAGE_DYNAMIC;                // write access access by CPU and GPU
   bd.ByteWidth = sizeof(TextureVertex)* 8;             // size is the VERTEX struct * 3
   bd.BindFlags = D3D11_BIND_VERTEX_BUFFER;       // use as a vertex buffer
   bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;    // allow CPU to write in buffer
   DX::ThrowIfFailed(d3dDevice->CreateBuffer(&bd, NULL, &texVertexBuffer));       // create the buffer

   // block while load is not completed
   while (!isLoadCompleted())
      Sleep(10);
   if (!vmStarted)
      std::thread([this]() {startProgram(localContext); }).detach(); // this will block until the application ends         
   vmStarted = true;
   preRender();
   updateWS = false;
   rotatedTo = -1;
}

void Direct3DBase::setColor(int color)
{
   if (color == lastRGB) return;
   lastRGB = color;
   aa = ((color >> 24) & 0xFF) / 255.0f;
   rr = ((color >> 16) & 0xFF) / 255.0f;
   gg = ((color >> 8) & 0xFF) / 255.0f;
   bb = (color & 0xFF) / 255.0f;

   VertexColor vcolor;
   vcolor.color = XMFLOAT4(rr, gg, bb, aa);

   D3D11_MAPPED_SUBRESOURCE ms;

   d3dcontext->Map(pBufferColor, NULL, D3D11_MAP_WRITE_DISCARD, NULL, &ms);   // map the buffer
   memcpy(ms.pData, &vcolor, sizeof(VertexColor));                // copy the data
   d3dcontext->Unmap(pBufferColor, NULL);                                     // unmap the buffer

   d3dcontext->VSSetConstantBuffers(1, 1, &pBufferColor);
}

#define f255(x) ((float)x/255.0f)

#define SCREEN_SHIFTY (glShiftY-screenShiftY)

void Direct3DBase::fillShadedRect(TCObject g, int32 x, int32 y, int32 w, int32 h, PixelConv c1, PixelConv c2, bool horiz)
{
   int clip[4] = { Graphics_clipX1(g), Graphics_clipY1(g), Graphics_clipX2(g), Graphics_clipY2(g) };
   y += SCREEN_SHIFTY;
   float x1 = (float)x, y1 = (float)y, x2 = x1 + w, y2 = y1 + h;
   XMFLOAT4 color1 = XMFLOAT4(f255(c2.r), f255(c2.g), f255(c2.b), f255(c2.a));
   XMFLOAT4 color2 = XMFLOAT4(f255(c1.r), f255(c1.g), f255(c1.b), f255(c1.a));
   VertexPositionColor cubeVertices[] = // position, color
   {
      { XMFLOAT2(x1, y1), horiz ? color1 : color1 },
      { XMFLOAT2(x2, y1), horiz ? color2 : color1 },
      { XMFLOAT2(x2, y2), horiz ? color2 : color2 },
      { XMFLOAT2(x1, y2), horiz ? color1 : color2 },
   };

   setProgram(PROGRAM_LC);
   setClip(clip);
   D3D11_MAPPED_SUBRESOURCE ms;
   d3dcontext->Map(pBufferRectLC, NULL, D3D11_MAP_WRITE_DISCARD, NULL, &ms);   // map the buffer
   memcpy(ms.pData, cubeVertices, sizeof(cubeVertices));                // copy the data
   d3dcontext->Unmap(pBufferRectLC, NULL);                                     // unmap the buffer

   UINT stride = sizeof(VertexPositionColor);
   UINT offset = 0;
   d3dcontext->IASetVertexBuffers(0, 1, &pBufferRectLC, &stride, &offset);
   d3dcontext->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
   d3dcontext->IASetIndexBuffer(indexBuffer, DXGI_FORMAT_R16_UINT, 0);
   d3dcontext->DrawIndexed(6, 0, 0);
}

void Direct3DBase::drawLine(int x1, int y1, int x2, int y2, int color)
{
   y1 += SCREEN_SHIFTY;
   y2 += SCREEN_SHIFTY;
   VertexPosition cubeVertices[] = // position, color
   {
      { XMFLOAT2((float)x1, (float)y1) },
      { XMFLOAT2((float)x2, (float)y2) },
   };

   setProgram(PROGRAM_GC);
   D3D11_MAPPED_SUBRESOURCE ms;
   d3dcontext->Map(pBufferRect, NULL, D3D11_MAP_WRITE_DISCARD, NULL, &ms);   // map the buffer
   memcpy(ms.pData, cubeVertices, sizeof(cubeVertices));                // copy the data
   d3dcontext->Unmap(pBufferRect, NULL);                                     // unmap the buffer

   UINT stride = sizeof(VertexPosition);
   UINT offset = 0;
   d3dcontext->IASetVertexBuffers(0, 1, &pBufferRect, &stride, &offset);
   d3dcontext->IASetIndexBuffer(indexBuffer, DXGI_FORMAT_R16_UINT, 0);
   d3dcontext->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_LINELIST);
   setColor(color);
   d3dcontext->DrawIndexed(2, 0, 0);
}

void Direct3DBase::fillRect(int x1, int y1, int x2, int y2, int color)
{
   y1 += SCREEN_SHIFTY;
   y2 += SCREEN_SHIFTY;
   VertexPosition cubeVertices[] = // position, color
   {
      { XMFLOAT2((float)x1, (float)y1) },
      { XMFLOAT2((float)x2, (float)y1) },
      { XMFLOAT2((float)x2, (float)y2) },
      { XMFLOAT2((float)x1, (float)y2) },
   };

   setProgram(PROGRAM_GC);
   D3D11_MAPPED_SUBRESOURCE ms;
   d3dcontext->Map(pBufferRect, NULL, D3D11_MAP_WRITE_DISCARD, NULL, &ms);   // map the buffer
   memcpy(ms.pData, cubeVertices, sizeof(cubeVertices));                // copy the data
   d3dcontext->Unmap(pBufferRect, NULL);                                     // unmap the buffer

   UINT stride = sizeof(VertexPosition);
   UINT offset = 0;
   d3dcontext->IASetVertexBuffers(0, 1, &pBufferRect, &stride, &offset);
   d3dcontext->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
   d3dcontext->IASetIndexBuffer(indexBuffer, DXGI_FORMAT_R16_UINT, 0);
   setColor(color);
   d3dcontext->DrawIndexed(6, 0, 0);
}

void Direct3DBase::drawPixels(float* glcoords, float* glcolors, int count, int color)
{
   int i;
   int n = count * 2;
   VertexPosition *cubeVertices = new VertexPosition[n], *cv = cubeVertices;// position, color
   XMFLOAT3 xcolor = XMFLOAT3(rr, gg, bb);
   for (i = count; --i >= 0;)
   {
      float x = *glcoords++;  // TODO use glcolors
      float y = *glcoords++;
      cv->pos = XMFLOAT2(x, y + SCREEN_SHIFTY); cv++;
      cv->pos = XMFLOAT2(x + 1, y + 1 + SCREEN_SHIFTY); cv++;
   }
   setProgram(PROGRAM_GC);

   if (n > lastPixelsCount)
   {
      DXRELEASE(pixelsIndexBuffer);
      DXRELEASE(pBufferPixels);
      // cache the pixels index
      unsigned short *cubeIndexes = new unsigned short[n];
      for (i = n; --i >= 0;) cubeIndexes[i] = i;
      lastPixelsCount = n;

      D3D11_SUBRESOURCE_DATA indexBufferData = { cubeIndexes,0,0 };
      CD3D11_BUFFER_DESC indexBufferDesc(sizeof(cubeIndexes[0]) * n, D3D11_BIND_INDEX_BUFFER);
      DX::ThrowIfFailed(d3dDevice->CreateBuffer(&indexBufferDesc, &indexBufferData, &pixelsIndexBuffer));
      delete cubeIndexes;

      D3D11_BUFFER_DESC bd = { 0 };
      bd.Usage = D3D11_USAGE_DYNAMIC;                // write access access by CPU and GPU
      bd.ByteWidth = sizeof(VertexPosition)* n;             // size is the VERTEX struct * 3
      bd.BindFlags = D3D11_BIND_VERTEX_BUFFER;       // use as a vertex buffer
      bd.CPUAccessFlags = D3D11_CPU_ACCESS_WRITE;    // allow CPU to write in buffer
      d3dDevice->CreateBuffer(&bd, NULL, &pBufferPixels);       // create the buffer
   }

   D3D11_MAPPED_SUBRESOURCE ms;
   d3dcontext->Map(pBufferPixels, NULL, D3D11_MAP_WRITE_DISCARD, NULL, &ms);   // map the buffer
   memcpy(ms.pData, cubeVertices, sizeof(VertexPosition)* n);                // copy the data
   d3dcontext->Unmap(pBufferPixels, NULL);                                     // unmap the buffer
   UINT stride = sizeof(VertexPosition);
   UINT offset = 0;
   d3dcontext->IASetVertexBuffers(0, 1, &pBufferPixels, &stride, &offset);
   d3dcontext->IASetIndexBuffer(pixelsIndexBuffer, DXGI_FORMAT_R16_UINT, 0);
   d3dcontext->IASetPrimitiveTopology(D3D10_PRIMITIVE_TOPOLOGY_LINELIST);
   setColor(color);
   d3dcontext->DrawIndexed(n, 0, 0);

   delete cubeVertices;
}

bool Direct3DBase::isLoadCompleted() 
{
   return loadCompleted == TASKS_COMPLETED;
}

void Direct3DBase::lifeCycle(bool suspending)
{
   postOnMinimizeOrRestore(minimized = suspending);
   if (minimized)
   {
      debug("==================================");
      recreateTextures();
   }
}

void Direct3DBase::updateScreen()
{
   if (minimized) return;
   d3dcontext->FinishCommandList(FALSE, &d3dCommandList); // 0ms
   updateScreenWaiting = true;
   PhoneDirect3DXamlAppComponent::Direct3DBackground::GetInstance()->RequestNewFrame();
   while (updateScreenWaiting) Sleep(0); // 16ms
   preRender();
}

void Direct3DBase::preRender()
{
   if (minimized) return;
   // Set the rendering viewport to target the entire window.
   CD3D11_VIEWPORT viewport(0.0f, 0.0f, (float)appW, (float)appH);
   d3dcontext->RSSetViewports(1, &viewport);

   d3dcontext->ClearRenderTargetView(renderTexView, clearColor);
   d3dcontext->ClearDepthStencilView(depthStencilView, D3D11_CLEAR_DEPTH, 1.0f, 0);

   d3dcontext->OMSetDepthStencilState(depthDisabledStencilState, 1);
   d3dcontext->OMSetBlendState(pBlendState, 0, 0xffffffff);

   d3dcontext->OMSetRenderTargets(1, &renderTexView, depthStencilView);
   d3dcontext->UpdateSubresource(constantBuffer, 0, NULL, &constantBufferData, 0, 0);
   curProgram = PROGRAM_NONE;
   d3dcontext->RSSetState(pRasterStateDisableClipping);
   clipSet = false;
}

void Direct3DBase::setProgram(whichProgram p)
{
   if (p == curProgram) return;
   lastRGB = 0xFAFFFFFF; // user may never set to this color
   curProgram = p;
   clipRect.right = -1;
   d3dcontext->RSSetState(pRasterStateDisableClipping);
   clipSet = false;
   switch (p)
   {
      case PROGRAM_GC:
         d3dcontext->VSSetShader(vertexShader, nullptr, 0);
         d3dcontext->PSSetShader(pixelShader, nullptr, 0);
         d3dcontext->IASetInputLayout(inputLayout);
         break;
      case PROGRAM_LC:
         d3dcontext->VSSetShader(vertexShaderLC, nullptr, 0);
         d3dcontext->PSSetShader(pixelShaderLC, nullptr, 0);
         d3dcontext->IASetInputLayout(inputLayoutLC);
         break;
      case PROGRAM_TEX:
         d3dcontext->PSSetSamplers(0, 1, &texsampler);
         d3dcontext->UpdateSubresource(constantBuffer, 0, nullptr, &constantBufferData, 0, 0);
         d3dcontext->VSSetShader(vertexShaderT, nullptr, 0);
         d3dcontext->PSSetShader(pixelShaderT, nullptr, 0);
         d3dcontext->IASetInputLayout(inputLayoutT);
         d3dcontext->IASetIndexBuffer(indexBuffer, DXGI_FORMAT_R16_UINT, 0);
         d3dcontext->IASetPrimitiveTopology(D3D11_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
         break;
   }
   d3dcontext->VSSetConstantBuffers(0, 1, &constantBuffer);
}

void Direct3DBase::loadTexture(Context currentContext, TCObject img, int32* textureId, Pixel *pixels, int32 width, int32 height, bool updateList)
{
   int32 i;
   PixelConv* pf = (PixelConv*)pixels;
   PixelConv* pt = (PixelConv*)xmalloc(width*height * 4), *pt0 = pt;
   ID3D11Texture2D *texture;
   D3D11_TEXTURE2D_DESC textureDesc = { 0 };
   textureDesc.Width = width;
   textureDesc.Height = height;
   textureDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
   textureDesc.MipLevels = textureDesc.ArraySize = textureDesc.SampleDesc.Count = 1;
   textureDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;

   for (i = width*height; --i >= 0; pt++, pf++) { pt->a = pf->r; pt->b = pf->g; pt->g = pf->b; pt->r = pf->a; }
   D3D11_SUBRESOURCE_DATA textureSubresourceData = { 0 };
   textureSubresourceData.pSysMem = pt0;
   textureSubresourceData.SysMemPitch = textureDesc.Width * 4; // Specify the size of a row in bytes
   if (FAILED(d3dDevice->CreateTexture2D(&textureDesc, &textureSubresourceData, &texture)))
      throwException(currentContext, OutOfMemoryError, "Out of texture memory for image with %dx%d", width, height);
   else
   {
      ID3D11ShaderResourceView* textureView;
      D3D11_SHADER_RESOURCE_VIEW_DESC textureViewDesc;
      ZeroMemory(&textureViewDesc, sizeof(textureViewDesc));
      textureViewDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
      textureViewDesc.ViewDimension = D3D11_SRV_DIMENSION_TEXTURE2D;
      textureViewDesc.Texture2D.MipLevels = 1;
      d3dDevice->CreateShaderResourceView(&texture[0], &textureViewDesc, &textureView);
      xmoveptr(&textureId[0], &texture);
      xmoveptr(&textureId[1], &textureView);
   }
   xfree(pt0);
}

void Direct3DBase::deleteTexture(TCObject img, int32* textureId, bool updateList)
{
   ID3D11Texture2D *texture;
   ID3D11ShaderResourceView *textureView;
   xmoveptr(&texture, &textureId[0]);
   xmoveptr(&textureView, &textureId[1]);
   if (textureView)
      textureView->Release();
   if (texture)
      texture->Release();
}

void Direct3DBase::setClip(int32* clip)
{
   bool doClip = clip != null;
   if (!doClip && clipSet)
      d3dcontext->RSSetState(pRasterStateDisableClipping);
   else
   if (doClip)
   {
      if (!clipSet)
         d3dcontext->RSSetState(pRasterStateEnableClipping);
      if (clip[0] != clipRect.left || clip[1] != clipRect.top || clip[2] != clipRect.right || clip[3] != clipRect.bottom)
      {
         clipRect.left = clip[0];
         clipRect.top = clip[1] + SCREEN_SHIFTY;
         clipRect.right = clip[2];
         clipRect.bottom = clip[3] + SCREEN_SHIFTY;
         d3dcontext->RSSetScissorRects(1, &clipRect);
      }
   }
   clipSet = doClip;
}

void Direct3DBase::drawTexture(int32* textureId, int32 x, int32 y, int32 w, int32 h, int32 dstX, int32 dstY, int32 imgW, int32 imgH, PixelConv *color, int32* clip)
{
   ID3D11Texture2D *texture;
   ID3D11ShaderResourceView *textureView;

   xmoveptr(&texture, &textureId[0]);
   xmoveptr(&textureView, &textureId[1]);
   setProgram(PROGRAM_TEX);
   setColor(!color ? 0 : 0xFF000000 | (color->r << 16) | (color->g << 8) | color->b);

   setClip(clip);

   dstY += SCREEN_SHIFTY;
   int32 dstY2 = dstY + h;
   int32 dstX2 = dstX + w;

   float left = (float)x / (float)imgW, top = (float)y / (float)imgH, right = (float)(x + w) / (float)imgW, bottom = (float)(y + h) / (float)imgH; // 0,0,1,1

   // VERTEX BUFFER
   TextureVertex cubeVertices[] =
   {  // destination coordinates    source coordinates
      { XMFLOAT2((float)dstX,  (float)dstY),  XMFLOAT2(left, top) },
      { XMFLOAT2((float)dstX2, (float)dstY),  XMFLOAT2(right, top) },
      { XMFLOAT2((float)dstX2, (float)dstY2), XMFLOAT2(right, bottom) },
      { XMFLOAT2((float)dstX,  (float)dstY2), XMFLOAT2(left, bottom) },
   };
   D3D11_MAPPED_SUBRESOURCE ms;
   d3dcontext->Map(texVertexBuffer, NULL, D3D11_MAP_WRITE_DISCARD, NULL, &ms);   // map the buffer
   memcpy(ms.pData, cubeVertices, sizeof(cubeVertices));                // copy the data
   d3dcontext->Unmap(texVertexBuffer, NULL);                                     // unmap the buffer

   // Set the vertex and index buffers, and specify the way they define geometry.
   UINT stride = sizeof(TextureVertex);
   UINT offset = 0;
   d3dcontext->IASetVertexBuffers(0, 1, &texVertexBuffer, &stride, &offset);

   // Set the vertex and pixel shader stage state.
   d3dcontext->PSSetShaderResources(0, 1, &textureView);
   // Draw the cube.
   d3dcontext->DrawIndexed(6, 0, 0);
}
