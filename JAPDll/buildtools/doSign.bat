cd %1\buildtools\
%REM Ok the following line does nothing more than CODESIGNPASSWD=`UserInput.exe`
for /f "usebackq" %%i in (`UserInput.exe`) do @set CODESIGNPASSWD=%%i
echo %CODESIGNPASSWD%
signtool sign /f %CODESIGNCERT% /p %CODESIGNPASSWD% /d JAPdll.dll /du www.jondos.de /t http://timestamp.comodoca.com/authenticode %2