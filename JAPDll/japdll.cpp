/*
 * Funktionalität:
 * - ein mit Titel bezeichnetes Fenster "always on top" setzen und umgekehrt
 * - ein mit Titel bezeichnetes Fenster in den Taskbar schicken,
 *   Pfad zum Icon dafür in JAPICON festlegen
 * - das Fenster durch einen Klick auf das Icon im Taksbar wieder sichtbar machen,
 *   Icon verschwindet
 */
#if _MSC_VER > 1000
	#pragma once
#endif // _MSC_VER > 1000


// Fügen Sie hier Ihre Header-Dateien ein
#include <windows.h>
#define NOTIFYICONDATA_SIZE NOTIFYICONDATA_V1_SIZE 

#include "japdll_jni.h"
#include "resource.h"

#define WM_TASKBAREVENT WM_USER+1
#define BLINK_RATE 500


// globales Window Handle --> if !=null --> JAP is minimized
HWND g_hWnd;
// tmp Window Handle
HWND tmp_hWnd;

// globales Moule Handle
HINSTANCE hInstance;

// Variable zur Sicherung der "alten" WndProc
WNDPROC lpPrevWndFunc;

HANDLE hThread; //Handle for the Blinking-Thread
BOOL isBlinking;


BOOL APIENTRY DllMain( HINSTANCE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
					 )
{
    switch (ul_reason_for_call)
	{
		case DLL_PROCESS_ATTACH:
			hInstance=hModule;
			hThread=NULL;
			g_hWnd=NULL;
			tmp_hWnd=NULL;
			break;
		case DLL_THREAD_ATTACH:
			break;
		case DLL_THREAD_DETACH:
			break;
		case DLL_PROCESS_DETACH:
			break;
    }
    return TRUE;
}

/*
 * Zeigt g_hWnd (wieder) und entfernt das Icon aus dem Taskbar.
 * (Vorher sollte HideWindowInTaskbar aufgerufen worden sein)
 */
VOID ShowWindowFromTaskbar() 
{
	if(g_hWnd==NULL)
		return;

	// alte WndProc wieder setzen 
	SetWindowLong(g_hWnd,GWL_WNDPROC,(LONG)lpPrevWndFunc);

	// Icondaten vorbereiten
	NOTIFYICONDATA nid;
	nid.hWnd = g_hWnd;
	nid.cbSize = NOTIFYICONDATA_SIZE;
	nid.uID = IDI_JAP;
	nid.uFlags = 0;

	// Window (wieder) anzeigen und Icon aus Taskbar entfernen
	ShowWindow(g_hWnd, SW_SHOW);
	ShowWindow(g_hWnd, SW_RESTORE);
	Shell_NotifyIcon(NIM_DELETE, &nid);
	g_hWnd=NULL;
}

/*
 * Neue WndProc zum Handeln von Ereignissen, besonders WM_TASKBAREVENT, 
 * das gesendet wird wenn etwas an "unserem" Icon im Taskbar passiert.
 */
LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    switch(msg)
    {
        case WM_TASKBAREVENT:
					switch (lParam) 
					{
						case WM_LBUTTONUP: 
							ShowWindowFromTaskbar();
						break;
					}
					return 0;
        break;
        default:
            return CallWindowProc(lpPrevWndFunc,hwnd, msg, wParam, lParam);
    }
}

/*
 * Versteckt g_hWnd und erzeugt ein Icon im Taskbar.
 */
VOID HideWindowInTaskbar(HWND hWnd) 
{
	g_hWnd=hWnd;
	// alte WndProc sichern und neue setzen
	lpPrevWndFunc = (WNDPROC) GetWindowLong(g_hWnd,GWL_WNDPROC);
	SetWindowLong(g_hWnd,GWL_WNDPROC,(LONG)WndProc);
	
	// Icondaten vorbereiten
	NOTIFYICONDATA nid;
	nid.hWnd = g_hWnd;
	nid.cbSize = NOTIFYICONDATA_SIZE;
	nid.uID = IDI_JAP;
	nid.uFlags = NIF_MESSAGE | NIF_TIP | NIF_ICON;
	nid.uCallbackMessage = WM_TASKBAREVENT;
	strcpy(nid.szTip, "JAP");
	nid.hIcon = LoadIcon(hInstance,MAKEINTRESOURCE(IDI_JAP));

	// Window verstecken und Icon im Taskbar setzen
	ShowWindow(g_hWnd, SW_HIDE);
	Shell_NotifyIcon(NIM_ADD, &nid);
}

/*
 * Von EnumWindow aufgerufene Prozedur zum Finden eines Fensters anhand seines Titels.
 * Liefert false (EnumWindow bricht ab) wenn das Fenster gefunden wurde und setzt g_hWnd, 
 * liefert true andernfalls (EnumWindows iteriert weiter).
 */
BOOL CALLBACK FindWindowByCaption(HWND hWnd, LPARAM lParam) 
{
	char *caption;
	char *windowCaption;
	BOOL result;

	caption = (char *) malloc(255);
	windowCaption = (char *) lParam;
	
	GetWindowText(hWnd, caption, 255);

	if (strncmp(caption, windowCaption, 255) == 0) 
	{
		tmp_hWnd = hWnd;
		result = FALSE;
	} 
	else 
	{
		result = TRUE;
	}
	
	free(caption);
	return result;
}


DWORD WINAPI BlinkThread( LPVOID lpParam ) 
	{ 
		NOTIFYICONDATA nid;
		nid.hWnd = g_hWnd;
		nid.cbSize = NOTIFYICONDATA_SIZE;
		nid.uID = IDI_JAP;
		nid.uFlags = NIF_ICON;
		while (isBlinking) 
			{
				isBlinking = false;
				nid.hIcon = LoadIcon(hInstance,MAKEINTRESOURCE(IDI_JAP_BLINK));
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hWnd==NULL)
					break;
				nid.hIcon = LoadIcon(hInstance,MAKEINTRESOURCE(IDI_JAP));
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hWnd==NULL)
					break;
				nid.hIcon = LoadIcon(hInstance,MAKEINTRESOURCE(IDI_JAP_BLINK));
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hWnd==NULL)
					break;
				nid.hIcon = LoadIcon(hInstance,MAKEINTRESOURCE(IDI_JAP));
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
			}
			hThread=NULL;
			return 0;
} 

VOID OnTraffic() 
{
  DWORD dwThreadId;
	isBlinking = true;
	if (hThread == NULL) 
		{
			hThread = CreateThread(NULL, 0, BlinkThread, NULL, 0, &dwThreadId);                
		}
}


/************************************************************************************
									JNI Methoden
 ************************************************************************************/

JNIEXPORT void JNICALL Java_gui_JAPDll_setWindowOnTop_1dll
  (JNIEnv * env, jclass, jstring s, jboolean b)
{
	const char *str = env->GetStringUTFChars(s, 0);
	
	tmp_hWnd=NULL;
  EnumWindows(&FindWindowByCaption, (LPARAM) str);
	if (tmp_hWnd != NULL) 
		{
			if (b) 
				SetWindowPos(tmp_hWnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE);
			else
				SetWindowPos(tmp_hWnd, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE);
		}
  env->ReleaseStringUTFChars(s, str);	
  return;
}

JNIEXPORT void JNICALL Java_gui_JAPDll_hideWindowInTaskbar_1dll
  (JNIEnv * env, jclass, jstring s)
{
	const char *str = env->GetStringUTFChars(s, 0);
  tmp_hWnd=NULL;
	EnumWindows(&FindWindowByCaption, (LPARAM) str);

	if (tmp_hWnd!= NULL) 
		HideWindowInTaskbar(tmp_hWnd);
  env->ReleaseStringUTFChars(s, str);
}

/*
JNIEXPORT void JNICALL Java_gui_JAPDll_showWindowFromTaskbar_1dll
  (JNIEnv *env, jobject obj, jstring s)
	{
		ShowWindowFromTaskbar();
	}*/ 

JNIEXPORT void JNICALL Java_gui_JAPDll_onTraffic_1dll
  (JNIEnv *, jclass) 
{
	if (g_hWnd != NULL) 
	{
		OnTraffic();
	}
}
   
