BugreportCleaner
================

Tool that extracts exceptions and life of PID from bugreport file generated from android devices.



#Usage:
Single bugreport:

<code> bugcleaner.jar -p "com.my.package" -i "path/to/bugreport.txt" -o "path/to/output/clean_bugreport.txt\" -gc [optional] </code>

Directory of bugreports:

<code> bugcleaner.jar -p "com.my.package" -i "path/to/mybugreports" -gc [optional] </code>

