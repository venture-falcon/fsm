generate:
	rm -fr lib/src/main/kotlin/io/nexure/payment/*Kt.kt
	rm -fr lib/src/main/java
	mkdir -p lib/src/main/kotlin
	mkdir -p lib/src/main/java
	protoc --java_out=lib/src/main/kotlin --kotlin_out=lib/src/main/kotlin lib/src/main/proto/PaymentNotificationMessage.proto

build: generate
	./gradlew build

test: build
	./gradlew test
