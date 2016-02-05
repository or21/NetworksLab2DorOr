if not exist -d bin\
(md bin) 
javac -cp ".;./jars/*" -d bin/ src/*.java
md c:\serverroot\static
xcopy static c:\serverroot\static /E