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

#define JAPDLL_VERSION "00.02.003"

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
HWND g_hWnd=NULL;

//globales Handle fuer das Msg Window
HWND g_hMsgWnd=NULL;


// globales Moule Handle
HINSTANCE hInstance;

//globale Icon Handles...
HICON g_hiconJAP;
HICON g_hiconJAPBlink;
HICON g_hiconWindowSmall;
HICON g_hiconWindowLarge;

// Variable zur Sicherung der "alten" WndProc
//WNDPROC g_lpPrevWndFunc;

HANDLE g_hThread; //Handle for the Blinking-Thread
BOOL g_isBlinking;
VOID ShowWindowFromTaskbar() ;

//Stores the Filename of the DLL
TCHAR strModuleFileName[4100];

JavaVM *gjavavm=NULL;
DWORD WINAPI MsgProcThread( LPVOID lpParam ) 
	{
		g_hMsgWnd=CreateWindow("JAPDllWndClass",NULL,0,CW_USEDEFAULT,CW_USEDEFAULT,CW_USEDEFAULT,CW_USEDEFAULT,NULL,NULL,hInstance,NULL);
		if(g_hMsgWnd==NULL)
			{
				return FALSE;
			}
		MSG msg;
		while(GetMessage(&msg,NULL,0,0))
			{
				TranslateMessage(&msg);
				DispatchMessage(&msg);
			}
		return TRUE;
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
//  return CallWindowProc(g_lpPrevWndFunc,hwnd, msg, wParam, lParam);
			return DefWindowProc(hwnd,msg,wParam,lParam);
	}




BOOL createMsgWindowClass()
	{
		WNDCLASSEX wndclass;
		memset(&wndclass,0,sizeof(WNDCLASSEX));
		wndclass.cbSize=sizeof(wndclass);
		wndclass.lpfnWndProc=WndProc;
		wndclass.cbClsExtra=0;
		wndclass.cbWndExtra=0;
		wndclass.hInstance=hInstance;
		wndclass.hIcon=NULL;
		wndclass.hbrBackground=(HBRUSH)GetStockObject(WHITE_BRUSH);
		wndclass.hCursor=LoadCursor(NULL,IDC_ARROW);
		wndclass.hIconSm=NULL;
		wndclass.lpszClassName="JAPDllWndClass";
		wndclass.style=0;
		return (RegisterClassEx(&wndclass)!=0);
	}

BOOL APIENTRY DllMain( HINSTANCE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved)
	{
		int ret=0;
    switch (ul_reason_for_call)
			{
				case DLL_PROCESS_ATTACH:
					hInstance=hModule;
					g_hThread=NULL;
					g_hWnd=NULL;
					g_hMsgWnd=NULL;
					//g_hiconJAP=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP),IMAGE_ICON,16,16,LR_DEFAULTCOLOR);
					g_hiconJAPBlink=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP_BLINK),IMAGE_ICON,16,16,LR_DEFAULTCOLOR);
					g_hiconWindowSmall=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP),IMAGE_ICON,16,16,LR_DEFAULTCOLOR);
					g_hiconWindowLarge=(HICON)LoadImage(hInstance,MAKEINTRESOURCE(IDI_JAP),IMAGE_ICON,32,32,LR_DEFAULTCOLOR);
					g_hiconJAP=g_hiconWindowSmall;
					ret=GetModuleFileName(hModule,strModuleFileName,4096);
					if(ret==0||ret==4096)
						{
							memset(strModuleFileName,0,4096*sizeof(TCHAR));
						}
					return createMsgWindowClass();
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
		SetWindowPos(g_hWnd, HWND_TOP, 0, 0, 0, 0, SWP_NOSIZE|SWP_NOMOVE/*|SWP_SHOWWINDOW*/);
		
		//ShowWindow(g_hWnd, SW_SHOWNORMAL);
		JNIEnv* env=NULL;
		if(gjavavm!=NULL)
			{
				gjavavm->AttachCurrentThread(&env,NULL);
				if(env!=NULL)
					{
						jclass clazz=env->FindClass("gui/JAPDll");
						if(clazz!=NULL)
							{
								jmethodID mid=env->GetStaticMethodID(clazz,"showMainWindow","()J");
								if(mid!=NULL)
									env->CallStaticVoidMethodA(clazz,mid,NULL);
							}
					}
				gjavavm->DetachCurrentThread();
			}
		ShowWindow(g_hWnd,SW_SHOWNORMAL);
		// Icondaten vorbereiten
		NOTIFYICONDATA nid;
		nid.hWnd = g_hMsgWnd;
		nid.cbSize = NOTIFYICONDATA_SIZE;
		nid.uID = IDI_JAP;
		nid.uFlags = 0;

		Shell_NotifyIcon(NIM_DELETE, &nid);
		DestroyWindow(g_hMsgWnd);
		g_hMsgWnd=NULL;
		g_hWnd=NULL;
	}

/*
 * Versteckt g_hWnd und erzeugt ein Icon im Taskbar.
 */
bool HideWindowInTaskbar(HWND hWnd,JNIEnv * env, jclass clazz)
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
	g_hMsgWnd=NULL;
	DWORD dwThreadId;
	CreateThread(NULL, 0, MsgProcThread, NULL, 0, &dwThreadId); 
	i=10;
	while(g_hMsgWnd==NULL&&i>0)
		{
			Sleep(50);
			i--;
		}
	if(g_hMsgWnd==NULL)
		return false;
	// Icondaten vorbereiten
	NOTIFYICONDATA nid;
	nid.hWnd = g_hMsgWnd;//hWnd;
	nid.cbSize = NOTIFYICONDATA_SIZE;
	nid.uID = IDI_JAP;
	nid.uFlags = NIF_MESSAGE | NIF_TIP | NIF_ICON;
	nid.uCallbackMessage = WM_TASKBAREVENT;
	lstrcpy(nid.szTip, "JAP");
	nid.hIcon = g_hiconJAP;

	// Window verstecken
	env->GetJavaVM(&gjavavm);
	//jmethodID mid=env->GetStaticMethodID(clazz,"hiddeMainWindow","()J");
	//if(mid!=NULL)
	//	env->CallStaticVoidMethodA(clazz,mid,NULL);
	//else
	//	ShowWindow(hWnd, SW_HIDE);
	//Icon im Taskbar setzen
	if(Shell_NotifyIcon(NIM_ADD, &nid)!=TRUE)
		{
			DestroyWindow(g_hMsgWnd);
			g_hMsgWnd=NULL;
			ShowWindow(hWnd, SW_SHOW);
			return false;
		}
	g_hWnd=hWnd;
	return true;
}

/*
 * Versteckt g_hWnd und erzeugt ein Icon im Taskbar.
 */
bool SetWindowIcon(HWND hWnd) 
{
	if(hWnd==NULL)
		return false;
	return SendMessage( 
  hWnd,              // handle to destination window 
  WM_SETICON,               // message to send
  ICON_BIG,          // icon type
  (LPARAM)g_hiconWindowLarge           // handle to icon (HICON)
) &&	SendMessage( 
  hWnd,              // handle to destination window 
  WM_SETICON,               // message to send
  ICON_SMALL,          // icon type
  (LPARAM)g_hiconWindowSmall           // handle to icon (HICON)
);

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
		nid.hWnd = g_hMsgWnd;
		nid.cbSize = NOTIFYICONDATA_SIZE;
		nid.uID = IDI_JAP;
		nid.uFlags = NIF_ICON;
		while (g_isBlinking) 
			{
				g_isBlinking = false;
				nid.hIcon = g_hiconJAPBlink;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hMsgWnd==NULL)
					break;
				nid.hIcon = g_hiconJAP;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hMsgWnd==NULL)
					break;
				nid.hIcon = g_hiconJAPBlink;
				Shell_NotifyIcon(NIM_MODIFY, &nid);
				Sleep(BLINK_RATE);
				if(g_hMsgWnd==NULL)
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
  (JNIEnv * env, jclass javaclass, jstring s)
{
	t_find_window_by_name tmp;
	tmp.name=env->GetStringUTFChars(s, 0);
  tmp.hWnd=NULL;
	EnumWindows(&FindWindowByCaption, (LPARAM) &tmp);
	jboolean ret=true;
	if (tmp.hWnd!= NULL) 
		ret=HideWindowInTaskbar(tmp.hWnd,env,javaclass);
	else
		ret=false;
	env->ReleaseStringUTFChars(s,tmp.name);
	return ret;
}


JNIEXPORT jboolean JNICALL Java_gui_JAPDll_setWindowIcon_1dll (JNIEnv * env, jclass, jstring s)
{
	t_find_window_by_name tmp;
	tmp.name=env->GetStringUTFChars(s, 0);
  tmp.hWnd=NULL;
	EnumWindows(&FindWindowByCaption, (LPARAM) &tmp);
	jboolean ret=true;
	if (tmp.hWnd!= NULL) 
	{
		ret=SetWindowIcon(tmp.hWnd);
	}
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

JNIEXPORT jstring JNICALL Java_gui_JAPDll_getDllFileName_1dll
  (JNIEnv * env, jclass)
	{
		return env->NewStringUTF(strModuleFileName);
	}