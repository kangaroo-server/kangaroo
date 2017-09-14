# Kangaroo: An OAuth2 Server
[![Build Status](https://travis-ci.org/kangaroo-server/kangaroo.svg)](https://travis-ci.org/kangaroo-server/kangaroo)
[![codecov](https://codecov.io/gh/kangaroo-server/kangaroo/branch/develop/graph/badge.svg)](https://codecov.io/gh/kangaroo-server/kangaroo)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.krotscheck/kangaroo/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.krotscheck/kangaroo)

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

This is a Java8 maven project, with no divergences from the standard maven 
lifecycle. The following maven profiles are supported:

- `mvn clean install -p h2` (default) <br/>
  Tests are run against an in-memory h2 database. This is run in travis.
- `mvn clean install -p mariadb` <br/>
  Tests are run against a MariaDB instance at `mysql://localhost:3306/`. The 
  root password is expected to be empty, so please secure your system 
  appropriately.

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
