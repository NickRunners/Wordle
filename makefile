default:
	javac -cp ".:Server/lib/gson-2.10.1.jar" Server/*.java
	javac Client/*.java

s:
	javac -cp ".:Server/lib/gson-2.10.1.jar" Server/*.java
	java -cp ".:Server/lib/gson-2.10.1.jar" Server/ServerMain

c:
	javac Client/*.java
	java Client/ClientMain

clean:
	rm Client/*.class
	rm Server/*.class