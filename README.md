# certmaker #

This web app generates a CA certificate, a couple of self-signed certificates and packages the certificates in keystores. Among the
generated items are:

- a CA certificate
- a CA key
- a server certificate signed by the generated CA
- a server key
- a PKCS12 keystore containing the server certificate
- a JKS keystore containing the server certificate

This may be useful if you wish to test SSL in your apps.

## Security ##

This app runs over HTTP by default, and passes the passwords you specify as command line parameters to keytool.

## Prerequisites ##

The openssl and keytool command line utilities are required.