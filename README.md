# javaChat

## Api
Firebase https://firebase.google.com/docs/admin/setup

Javamail https://www.cnblogs.com/qingwen/p/5513090.html

## Prerequisites

```
brew install maven
```

## Running the project
Go to javachat directory

### Build server
```
mvn install
```

### Run server
```
java -jar target/gs-maven-0.1.0.jar 5432
```

### Build client
in src/main/java

```
javac client/*.java
```

### Run client
in src/main/java

```
java client/ChatClient localhost 5432
```
