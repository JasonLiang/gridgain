::
:: Copyright (C) GridGain Systems Licensed under GPLv3, http://www.gnu.org/licenses/gpl.html
:: _________        _____ __________________        _____
:: __  ____/___________(_)______  /__  ____/______ ____(_)_______
:: _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
:: / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
:: \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
::
:: Version: 4.0.0c.22032012
::

::
:: Starts Scala REPL with GridGain on the classpath.
:: Note that 'scala' must be on PATH.
::

@echo off

if "%OS%" == "Windows_NT"  setlocal

:: Check JAVA_HOME.
if not "%JAVA_HOME%" == "" goto checkJdk
    echo %0, ERROR: JAVA_HOME environment variable is not found.
    echo %0, ERROR: Please create JAVA_HOME variable pointing to location of JDK 1.6 or JDK 1.7.
    echo %0, ERROR: You can also download latest JDK at: http://java.sun.com/getjava
goto error_finish

:checkJdk
:: Check that JDK is where it should be.
if exist "%JAVA_HOME%\bin\java.exe" goto checkJdkVersion
    echo %0, ERROR: The JDK is not found in %JAVA_HOME%.
    echo %0, ERROR: Please modify your script so that JAVA_HOME would point to valid location of JDK.
goto error_finish

:checkJdkVersion
"%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr "1\.[67]\." > nul
if %ERRORLEVEL% equ 0 goto checkGridGainHome1
    echo %0, ERROR: The version of JAVA installed in %JAVA_HOME% is incorrect.
    echo %0, ERROR: Please install JDK 1.6 or 1.7.
    echo %0, ERROR: You can also download latest JDK at: http://java.sun.com/getjava
goto error_finish

:: Check GRIDGAIN_HOME.
:checkGridGainHome1
if not "%GRIDGAIN_HOME%" == "" goto checkGridGainHome2
    echo %0, WARN: GRIDGAIN_HOME environment variable is not found.
    pushd "%~dp0"/..
    set GRIDGAIN_HOME=%CD%
    popd

:checkGridGainHome2
:: remove all trailing slashes from GRIDGAIN_HOME.
if %GRIDGAIN_HOME:~-1,1% == \ goto removeTrailingSlash
if %GRIDGAIN_HOME:~-1,1% == / goto removeTrailingSlash
goto checkGridGainHome3
:removeTrailingSlash
set GRIDGAIN_HOME=%GRIDGAIN_HOME:~0,-1%
goto checkGridGainHome2

:checkGridGainHome3
if exist "%GRIDGAIN_HOME%\config" goto run
    echo %0, ERROR: GRIDGAIN_HOME environment variable is not valid installation home.
    echo %0, ERROR: GRIDGAIN_HOME variable must point to GridGain installation folder.
goto error_finish
:run

:: This is Ant-augmented variable.
set ANT_AUGMENTED_GGJAR=gridgain-4.0.0c.jar

::
:: Set GRIDGAIN_LIBS
::
call "%GRIDGAIN_HOME%\bin\setenv.bat"

set CP=%GRIDGAIN_LIBS%;%GRIDGAIN_HOME%\%ANT_AUGMENTED_GGJAR%

::
:: Process 'verbose' mode and optional Spring configuration file.
::
if [%1] == [-v] (
    set QUIET=-DGRIDGAIN_QUIET=false
) else (
    set QUIET=-DGRIDGAIN_QUIET=true
)

::
:: Set program name.
::
set PROG_NAME=gridgain.bat
if "%OS%" == "Windows_NT" set PROG_NAME=%~nx0%

::
:: Start REPL.
::
"%JAVA_HOME%\bin\java.exe" -Xss2m  -DGRIDGAIN_SCRIPT -DGRIDGAIN_HOME="%GRIDGAIN_HOME%" -DGRIDGAIN_PROG_NAME="%PROG_NAME%" %QUIET% -cp "%CP%" scala.tools.nsc.MainGenericRunner -usejavacp -Yrepl-sync -i "%GRIDGAIN_HOME%\bin\scalar.scala"

:error_finish
