@REM Maven Wrapper Script for Windows
@REM Licensed to the Apache Software Foundation (ASF)

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0")

@SET WRAPPER_JAR="%BASE_DIR%.mvn\wrapper\maven-wrapper.jar"
@SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
@SET WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

@IF EXIST %WRAPPER_JAR% (
  @REM wrapper jar already present
) ELSE (
  @echo Downloading Maven wrapper jar...
  @powershell -Command "&{[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $webclient=New-Object System.Net.WebClient; $webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')}"
)

@"%JAVA_HOME%\bin\java.exe" ^
  %MAVEN_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%BASE_DIR%" ^
  %WRAPPER_LAUNCHER% %*
