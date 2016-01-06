if not exist -d bin\
(md bin) 
javac -d bin/ src/*.java
md c:\serverroot\static
xcopy static c:\serverroot\static /E