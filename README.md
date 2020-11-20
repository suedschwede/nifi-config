

# Deploy and configure Template on Nifi (Compatibel to NIFI 1.12.1)

Update, Extract Nifi Configuration


## How to :

```text
usage: java -jar nifi-deploy-config.jar [OPTIONS]
 -h,--help                 Usage description
 -b,--branch <arg>         Target process group (must begin by root) : root > my group > my sub group (default : root)
 -m,--mode <arg>           mandatory, possible values : updateConfig/extractConfig/deployTemplate/undeploy/extractParameter/updateParameter
 -c,--conf <arg>           mandatory if mode in [updateConfig, extractConfig, deployTemplate]  : configuration file
 -n,--nifi <arg>           mandatory : Nifi URL (ex : http://localhost:8080/nifi-api)
```

*For more options see Chapter [Advanced options](#advanced-options)*

Requirement : *You must have java 8 or higher installed on your machine*

## Step by Step : use in real life


#NIFI Deployment

- undeploy ProcessGroup
- create/update ParameterContext (change parameters manually)
- upload and deploy template
- update config (set Parameter Context in ProcessGroups, update sensitive values in Controller Services)


### Prepare your nifi development

1 ) Create a template on nifi :

with this rule : each processor and each controller in a process group **must** have a unique name.

![template](/docs/template.png)

2) Download it

3) Extract a sample configuration with the command

```shell
java -jar nifi-deploy-config.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>My Group>My Subgroup" \
  -conf /tmp/config.json \
  -mode extractConfig
```

### Deploy it on production

1a) undeploy the old version with the command

```shell
java -jar nifi-deploy-config.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -m undeploy
```

1b) deploy the template with the command

```shell
java -jar nifi-deploy-config.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -conf /tmp/my_template.xml \
  -m deployTemplate
```

2) update the production configuration with the command

```shell
java -jar nifi-deploy-config.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>My group>My Subgroup" \
  -conf /tmp/PROD_config.json \
  -mode updateConfig
```


### Sample usage

#### Sample extract configuration

```shell
java -jar nifi-deploy-config.jar \
  -nifi http://ip-nifi-dev:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode extractConfig
```

#### Sample update configuration

```shell
java -jar nifi-deploy-config.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -mode updateConfig
```

#### Sample deploy Template

```shell
java -jar nifi-deploy-config.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/my_template.xml \
  -m deployTemplate
```

#### Sample undeploy

```shell
java -jar nifi-deploy-config.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -m undeploy
```

force mode actived

```shell
java -jar nifi-deploy-config.jar \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -m undeploy
  -f
  -timeout 600
  -interval 10
```

#### Sample access via username/password

```shell
java -jar nifi-deploy-config.jar \
  -user my_username \
  -password my_password \
  -nifi http://ip-nifi-prod:8080/nifi-api \
  -branch "root>my group>my subgroup" \
  -conf /tmp/test2.json \
  -m updateConfig
```



### Advanced Options

#### Pooling

```text
 -timeout <arg>            allows specifying the polling timeout in second (defaut 120 seconds); negative values indicate no timeout
 -interval <arg>           allows specifying the polling interval in second (default 2 seconds)
```

####  Security

 ```text
 -password <arg>           password for access via username/password, then user is mandatory
 -user <arg>               user name for access via username/password, then password is mandatory
 -accessFromTicket         Access via Kerberos ticket exchange / SPNEGO negotiation 
 -VerifySsl                turn on ssl verification certificat 
 ```


####  Timeout Api Client

 ```text
 -connectionTimeout <arg>  configure api client connection timeout (default 10 seconds)
 -readTimeout <arg>        configure api client read timeout (default 10 seconds)
 -writeTimeout <arg>       configure api client write timeout (default 10 seconds)
 ```

####  Position

```text
 -placeWidth <arg>         width of place for installing group (default 1935 : 430 * (4 + 1/2) = 4 pro line)
 -startPosition <arg>      starting position for the place for installing group, format x,y (default : 0,0)
```

####  Other

 ```text
 -f,--force                turn on force mode : empty queue after timeout
 -noStartProcessors        turn off auto start of the processors after update of the config
 -enableDebugMode          turn on debugging mode of the underlying API library
 -keepTemplate             keep template after installation (default false)
 ```
## Note

#### About controller

By default, nifi-config uses the controller declared on the parent group that has the same name, if any then deletes the controller declaration on the child group, otherwise uses the controller of the group.

If you want to use a controller declared on parent group without updating it, just declare the controller with no property on json file : 

    "controllerServices": [
     {
      "name": "DBCPConnectionPool"
      }
