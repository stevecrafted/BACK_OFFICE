@echo off
setlocal enabledelayedexpansion

call mvn install:install-file ^
  -Dfile=src/main/webapp/WEB-INF/lib/spring-init-framework-1.0.0.jar ^
  -DgroupId=com.framework ^
  -DartifactId=spring-init-framework ^
  -Dversion=1.0.0 ^
  -Dpackaging=jar

call mvn clean package
call "%JAVA_HOME%\bin\java" -jar target/spring-init-test-1.0.0.jar

endlocal