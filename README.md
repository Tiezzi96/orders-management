# Orders Management Project
---

Il progetto **Orders Management** è stato sviluppato per l'esame di Advanced Programming Techniques dell'Università degli studi di Firenze. Il progetto consiste in un'applicazione Java per la gestione di clienti e dei rispettivi ordini di un'attività commerciale. 

---

# Ambiente e Tecnologie di Sviluppo
Il progetto è stato implementato tramite l'utilizzo dell'ambiente di sviluppo **Eclipse** e del linguaggio Java 8. L'applicazione è di tipo desktop per cui non richiede connessione internet, ad eccezione della fase iniziale di installazione delle dipendenze necessarie. Per eseguire il codice è necessario siano installati:

- Maven: gestione delle dipendenze
- Docker: gestione dei container

---
# Esecuzione dell'applicazione
Al fine di eseguire l'applicazione è necessario:

- Costruire il file JAR (il quale viene generalmente generato nella directory **target/**) attraverso il comando:<br> `mvn -f orders-management/pom.xml clean package`
- L'applicazione necessita che **Docker Desktop** sia avviato e che il docker deamon sia in esecuzione. L'avvio dei container definito nel progetto avviene attraverso il comando: <br> `mvn -f orders-management/pom.xml docker:start`
- Successivamente è possibile avviare l'applicazione, attraverso il comando: <br> `java -jar orders-management/target/orders-management-0.0.1-SNAPSHOT-jar-with-dependencies.jar`

Per avviare l'applicazione è possibile definire dei paramentri da passare da linea di comando, utilizzando la libreria **[Picocli](https://picocli.info/)** per gestire tali parametri. In questo modo è possibile personalizzare l'avvio specificando:

| Parametro | Descrizione | Valore di default | Esempio |
| --------- | ----------- | ----------------- | ------- |
| `--db-name` | nome del database | ordersmgmt | `--db-name ordersmgmt` |
| `--db-clients-collection` | collezione dei clienti | clients | `--db-clients-collection clients` |
| `--db-orders-collection` | collezione degli ordini | orders | `--db-orders-collection orders` |
| `--mongo-port` | porta del MongoDB container | 27017 | `--mongo-port 27017` |
| `--mongo-host` | indirizzo dell'host di MongoDB | localhost | `--mongo-host localhost` |

---

# Badges

[![.github/workflows/ordersmgmt.yml](https://github.com/Tiezzi96/orders-management/actions/workflows/ordersmgmt.yml/badge.svg)](https://github.com/Tiezzi96/orders-management/actions)
[![Coverage Status](https://coveralls.io/repos/github/Tiezzi96/orders-management/badge.svg?branch=master)](https://coveralls.io/github/Tiezzi96/orders-management?branch=master)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Tiezzi96_orders-management&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Tiezzi96_orders-management)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Tiezzi96_orders-management&metric=bugs)](https://sonarcloud.io/summary/new_code?id=Tiezzi96_orders-management)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=Tiezzi96_orders-management&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=Tiezzi96_orders-management)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=Tiezzi96_orders-management&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=Tiezzi96_orders-management)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=Tiezzi96_orders-management&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=Tiezzi96_orders-management)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Tiezzi96_orders-management&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Tiezzi96_orders-management)

---

# Fase di Testing
---
## Esecuzione dei test
Per eseguire i test è necessario è necessario:
- assicurarsi che **Docker Desktop** sia avviato (il Docker deamon deve essere in esecuzione)
- posizionarsi nella root del progetto ed eseguire il comando:<br> `mvn -f orders-management/pom.xml clean verify`

Il comando permette di ripulire i target precedenti e lanciare la suite di test contenuto nal modulo *orders-management*. 
Per eseguire i test da Eclipse è possibile utilizzare la procedura `Run As -> Junit Test`. Per gli Integration e gli E2E test deve essere avviato il comando `mvn -f orders-management/pom.xml docker:start` in precedenza.


## Esecuzione del Code Coverage e del Mutation Testing
Al fine di eseguire il calcolo della code coverage ed il mutation testing, devono essere eseguiti il plugin jacoco ed il plugin pit. A tal fine il comando precedente deve essere modificato come segue, attivando i rispettivi profili:
`mvn -f orders-management/pom.xml clean verify -Pjacoco -Ppitest`

---

Tutti i comandi Maven (mvn ...) e Java (java ...) mostrati sono stati verificati in **Windows PowerShell**




