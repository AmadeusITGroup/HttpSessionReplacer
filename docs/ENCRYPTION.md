# Session Encryption

Session data can be encrypted when stored into repository. This allows secure
transport of the data over unsecured lines.

Encryption uses AES CBC algorithm and a key specicfied in configuration. The
key can be provided either as plaintext or as file or resource on specified
URL. The key is provided using system property or servlet initialization
parameter `com.amadeus.session.encryption.key`. If its value is a URL, the
implementation will try to load key from the specified target. Key loading is
supported from `file`, `http` or `https` schemas. 

## Cryptography Notes

The AES key is initialized as SHA-256 from the provided encryption key. 

Initialization vector for AES is generated for every serialization of the 
session data. The initialization vector is stored as 16 bytes binary prefix to
the session data.

If the provided key is specified as plaintext it will be present in the memory
during whole lifetime of the application. Keys loaded from a URL are present
in the memory only during encrpytion initialization. AES key is present in 
memory during lifetime of the application.