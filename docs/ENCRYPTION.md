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

Initialization vector for AES is generated for every serialization of the 
session data. The initialization vector is stored as 16 bytes binary prefix to
the session data.

Implementation has no specific mitigation against timing attacks. Note that 
without access to the code, it is difficult to know plaintext format of session
data, and it might also be difficult to force specific format.

If key is specified as plaintext it will be present in the memory during whole
lifetime of the application. Keys loaded from a URL are present in the memory 
only during encrpytion initialization.