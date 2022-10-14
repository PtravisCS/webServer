## README 

### Comilation
  The program was created in Netbeans, though I don't forsee any problems with command line compilation.
  
### Notes
  The server looks at its local directory for the files, (unless it's compiled in netbeans, then it looks at the project root directory)
  The server should not honour a request for a directory instead sending a 403. Valid files are .ico, .png, .html, .css, and .js, all other files
  will result in 404.

  A highly rudimentary web site was included with the project files in order to allow for easier testing. hello.html is the index page.

### Wireshark
  The Wireshark captures should be filtered already (I exported them with the filters).
