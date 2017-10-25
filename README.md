# Sonar-Hystrix - Sonar rules for Hystrix

Three Hystrix rule included:
* Hystrix subclass
* Spring Hystrix Annotation
* Spirng Feign

## Build

mvn clean package

## Usage

* Put the **jar** to **extentions/plugin**
* Enable the rule with role **admin**
* Config **sonar-project.properties**
* Run **sonar-scanner**

## compatibility

### sonar-java-plugin

* version 4.14.0.11784 may only be compatible with sonar-6.x
* version 4.7.1.927 is compatible with both sonar-5.6.x and sonar-6.x
