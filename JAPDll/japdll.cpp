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

#define JAPDLL_VERSION "00.01.006"

// Fügen Sie hier Ihre Header-Dateien ein
#include <windows.h>
#define NOTIFYICONDATA_SIZE NOTIFYICONDATA_V1_SIZE 

#include "japdll_jni.h"
#include "resource.h"

#define WM_TASKBAREVENT WM_USER+100
#define BLINK_RATE 500

struct t_find_window_by_name
	{
		const char * name;
		HWND hWnd;
	};
	

// globales Window Handle --> if !=null --> JAP is minimized
HWND g_hWnd;

// globales Moule Handle
HINSTANCE hInstance;

//globale Icon Handles...
HICON g_hiconJAP;
HICON g_hiconJAPBlink;

// Variable zur Sicherung der "alten" WndProc
WNDPROC g_lpPrevWndFunc;

HANDLE g_hThread; //Handle for the Blinking-Thread
BOOL g_isBlinking;
VOID ShowWindowFromTaskbar() ;



DWORD WINAPI SetOldWndProcThread( LPVOID lpParam ) 
	{
		Sleep(500);
		SetWindowLongPtr(g_hWnd,GWL_WNDPROC,(LONG_PTR)g_lpPrevWndFunc);
		g_hWnd=NULL;
		return 0;
	}

/*
 * Neue WndProc zum Handeln von Ereignissen, besonders WM_TASKBAREVENT, 
 * das gesendet wird wenn etwas an "unserem" Icon im Taskbar passiert.
 */
LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
	{
		if(msg==WM_TASKBAREVENT)
			{
				if(lParam==WM_LBUTTONUP)
 					ShowWindowFromTaskbar();
				return 0;
			}  
    return CallWindowProc(g_lpPrevWndFunc,hwnd, msg, wParam, lParam);
	}

BOOL APIENTRY DllMain( HINSTANCE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved)
	{
    switch (ul_reason_for_call)
			{
				case DLL_PROCESS_ATTACH:
					hInstance=hModule;
					g_hThread=NULL;
					g_hWnd=NULL;
					g_hiconJAP=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP),IMAGE_ICON,16,16,LR_DEFAULTCOLOR);
					g_hiconJAPBlink=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP_BLINK),IMAGE_ICON,16,16,LR_DEFAULTCOLOR);
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
		SetWindowPos(g_hWnd, HWND_TOP, 0, 0, 0, 0, SWP_NOSIZE|SWP_NOMOVE|SWP_SHOWWINDOW);
		ShowWindow(g_hWnd, SW_SHOWNORMAL);

		// Icondaten vorbereiten
		NOTIFYICONDATA nid;
		nid.hWnd = g_hWnd;
		nid.cbSize = NOTIFYICONDATA_SIZE;
		nid.uID = IDI_JAP;
		nid.uFlags = 0;

		Shell_NotifyIcon(NIM_DELETE, &nid);
		
		//Async old WndProc wieder setzen
		DWORD dwThreadId;
		CreateThread(NULL, 0, SetOldWndProcThread, NULL, 0, &dwThreadId); 
	}

/*
 * Versteckt g_hWnd und erzeugt ein Icon im Taskbar.
 */
bool HideWindowInTaskbar(HWND hWnd) 
{
	if(hWnd==NULL)
		return false;
	int i=10;
	//Warten falls g_hWnd noch von letzten Aufruf "blockiert"
	while(g_hWnd!=NULL&&i>0)
		{
			Sleep(100);
			i--;
		}
	if(g_hWnd!=NULL)
		return false;

	// Icondaten vorbereiten
	NOTIFYICONDATA nid;
	nid.hWnd = hWnd;
	nid.cbSize = NOTIFYICONDATA_SIZE;
	nid.uID = IDI_JAP;
	nid.uFlags = NIF_MESSAGE | NIF_TIP | NIF_ICON;
	nid.uCallbackMessage = WM_TASKBAREVENT;
	lstrcpy(nid.szTip, "JAP");
	nid.hIcon = g_hiconJAP;

	//LoadIcon(hInstance,MAKEINTRESOURCE(IDI_JAP));
	if(nid.hIcon==NULL)
		return false;
	// Window verstecken
	ShowWindow(hWnd, SW_HIDE);

	//Icon im Taskbar setzen

	if(Shell_NotifyIcon(NIM_ADD, &nid)!=TRUE)
		{
			ShowWindow(hWnd, SW_SHOW);
			return false;
		}

	//neu WndProc setzen
	LONG_PTR tmpPtr=SetWindowLongPtr(hWnd,GWL_WNDPROC,(LONG_PTR)WndProc);
	if(tmpPtr==NULL)
		{
			Shell_NotifyIcon(NIM_DELETE, &nid);
			ShowWindow(hWnd, SW_SHOW);
			return false;
		}
	//globales Handle und alte Prozedur setzen
	g_lpPrevWndFunc = (WNDPROC) tmpPtr;
	g_hWnd=hWnd;

	return true;
}

/*
 * Von EnumWindow aufgerufene Prozedur zum Finden eines Fensters anhand seines Titels.
 * Liefert false (EnumWindow bricht ab) wenn das Fenster gefunden wurde und setzt g_hWnd, 
 * liefert true andernfalls (EnumWindows iteriert weiter).
 */
BOOL CALLBACK FindWindowByCaption(HWND hWnd, LPARAM lParam) 
{
	char caption[255];
	t_find_window_by_name* pFindWindow=(t_find_window_by_name*)lParam;
	
	if(GetWindowText(hWnd, caption, 255)==0)
		return TRUE;

	if (lstrcmp(pFindWindow->name,caption) == 0) 
		{
			pFindWindow->hWnd = hWnd;
			return FALSE;
		} 
	return TRUE;
}


DWORD WINAPI BlinkThread( LPVOID lpParam ) 
	{ 
		NOTIFYICONDATA nid;
		nid.hWnd = g_hWnd;
		nid.cbSize = NOTIFYICONDATA_SIZE;
		nid.uID = IDI_JAP;
		nid.uFlags = NIF_ICON;
		while (g_isBlinking) 
			{
				g_isBlinking = false;
				nid.hIcon = g_hiconJAPBlink;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hWnd==NULL)
					break;
				nid.hIcon = g_hiconJAP;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hWnd==NULL)
					break;
				nid.hIcon = g_hiconJAPBlink;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hWnd==NULL)
					break;
				nid.hIcon = g_hiconJAP;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
			}
			g_hThread=NULL;
			return 0;
} 

VOID OnTraffic() 
{
  DWORD dwThreadId;
	g_isBlinking = true;
	if (g_hThread == NULL) 
		{
			g_hThread = CreateThread(NULL, 0, BlinkThread, NULL, 0, &dwThreadId);                
		}
}


/************************************************************************************
									JNI Methoden
 ************************************************************************************/

JNIEXPORT void JNICALL Java_gui_JAPDll_setWindowOnTop_1dll
  (JNIEnv * env, jclass, jstring s, jboolean b)
{
	t_find_window_by_name tmp;
	tmp.name=env->GetStringUTFChars(s, 0);
  tmp.hWnd=NULL;
	EnumWindows(&FindWindowByCaption, (LPARAM) &tmp);
	if (tmp.hWnd != NULL) 
		{
			if (b) 
				SetWindowPos(tmp.hWnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE);
			else
				SetWindowPos(tmp.hWnd, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOACTIVATE | SWP_NOMOVE | SWP_NOSIZE);
		}
  env->ReleaseStringUTFChars(s, tmp.name);	
  return;
}

JNIEXPORT jboolean JNICALL Java_gui_JAPDll_hideWindowInTaskbar_1dll
  (JNIEnv * env, jclass, jstring s)
{
	t_find_window_by_name tmp;
	tmp.name=env->GetStringUTFChars(s, 0);
  tmp.hWnd=NULL;
	EnumWindows(&FindWindowByCaption, (LPARAM) &tmp);
	jboolean ret=true;
	if (tmp.hWnd!= NULL) 
		ret=HideWindowInTaskbar(tmp.hWnd);
	else
		ret=false;
	env->ReleaseStringUTFChars(s,tmp.name);
	return ret;
}


JNIEXPORT void JNICALL Java_gui_JAPDll_onTraffic_1dll
  (JNIEnv *, jclass) 
{
	if (g_hWnd != NULL) 
	{
		OnTraffic();
	}
}

JNIEXPORT jstring JNICALL Java_gui_JAPDll_getDllVersion_1dll
  (JNIEnv * env, jclass)
	{
		return env->NewStringUTF(JAPDLL_VERSION);
	}
