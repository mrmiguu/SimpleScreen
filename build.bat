@echo off
javac -cp . com/mrmiguu/SimpleScreen.java
jar -cvf SimpleScreen.jar com/mrmiguu/*.class
pause