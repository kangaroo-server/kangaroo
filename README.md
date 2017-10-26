# Kangaroo: An OAuth2 Server
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.krotscheck/kangaroo/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.krotscheck/kangaroo) [![Build Status](https://jenkins.krotscheck.net/buildStatus/icon?job=Kangaroo/kangaroo/develop)](https://jenkins.krotscheck.net/job/Kangaroo/job/kangaroo/job/develop) [![Jenkins tests](https://img.shields.io/jenkins/t/https/jenkins.krotscheck.net/job/Kangaroo/job/kangaroo/job/develop.svg)](https://jenkins.krotscheck.net/job/Kangaroo/job/kangaroo/job/develop/) [![Known Vulnerabilities](https://snyk.io/test/github/kangaroo-server/kangaroo/badge.svg?targetFile=/pom.xml)](https://snyk.io/test/github/kangaroo-server/kangaroo?targetFile=/pom.xml)

Kangaroo is an open source, multi-tenant, OAuth2 Authorization server. It
fully supports RFC 6749 (The original OAuth2 Specification), and aspires to 
be a reference implementation thereof. We intend to expand the scope of this
project to include other OAuth2 related RFC's.

## For The Curious

If you are interested in exploring the capabilities of the project, 
simply run this command on a ready docker host, and direct your browser at 
[`http://localhost:8080/`](http://localhost:8080)

`docker run -d -p 8080:8080 kangaroo/kangaroo-master:latest`

Note: OAuth2 requires HTTP TLS encryption, so the standalone server will 
generate and sign its own certificate. Your browser will notify you of the 
untrusted nature of this certificate every time the process is restarted.

## Documentation

Detailed documentation on the project, including configuration options, 
deployment models, and upgrade patterns, please see our project website at 
[https://kangaroo-server.github.io](https://kangaroo-server.github.io).

## Developer Quickstart

### Prerequisites

The following items are required. Note that developing on windows is not 
supported.

- Java 8 and Maven 3.5+
- Google Chrome v62 or greater
- The google `chromedriver` available on your `$PATH`
- A facebook application (unpublished is ok)
- A testing google email account and oauth2 application.
- MySQL, exposed at `mysql://localhost:3306/`, with the root password blank, 
  or a `~/.my.cnf` client configuration with database creation credentials.

The following environment variables should be set.

```bash
export KANGAROO_FB_APP_USR=<your_facebook_app_id>
export KANGAROO_FB_APP_PSW=<your_facebook_app_secret>

export KANGAROO_GOOGLE_ACCOUNT_USR=<your_google_login>
export KANGAROO_GOOGLE_ACCOUNT_PSW=<your_google_password>
export KANGAROO_GOOGLE_APP_USR=<your_google_app_id>
export KANGAROO_GOOGLE_APP_PSW=<your_google_app_secret>
```

### Build and test the project.

This is a maven project, with no divergences from the standard maven 
lifecycle. The following maven profiles are supported:

- `mvn clean install -p h2` (default) <br/>
  Tests are run against an in-memory h2 database.
- `mvn clean install -p mariadb` <br/>
  Tests are run against a MariaDB instance at `mysql://localhost:3306/`.

### Community

The current community maintains a presence on FreeNode's
[#kangaroo](http://webchat.freenode.net/?channels=kangaroo) channel.

### How can I help?

We maintain an active list of [issues](https://github.com/kangaroo-server/kangaroo/issues), and use 
github's [project tracker](https://github.com/kangaroo-server/kangaroo/projects)
for larger bodies of work. To coordinate and ensure that your work is not 
being duplicated, please reach out the the community in the [IRC Channel](http://webchat.freenode.net/?channels=kangaroo)
channel.

### Philosophy

We adhere to the doctrine of [Choose Boring Technology](http://mcfunley.com/choose-boring-technology). 
This is done mostly out of a need for project sanity; if development, 
packaging, and deployment get too complex, we risk being labeled as an
"Experts only" project, reducing adoption and hampering forward motion.

We expect 100% test coverage. While we understand that this does not 
necessarily guarantee good tests, it does force a contributing engineer to 
think through edge cases in a way that might otherwise be skipped.
