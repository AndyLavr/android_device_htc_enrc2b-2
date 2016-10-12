
After pushing some binaries onto devices that run newer versions of Android, a frustrating error often occurs:
error: only position independent executables (PIE) are supported.
The error message is quite straightforward — the binary is not position independent. 


Things get more complicated if you only have the binary or it’s not feasbile to rebuild it with PIE support. 
Then this post can help. I stumbled across a workaround on a Chinese website. It’s very easy, even less painful than rebuilding the binary with PIE enabled.


Basically what you only need is a binary editor like 010 Editor. Use it open the binary file, count carefully to find the 17th byte, change the value 02 to 03, and that’s it!
$ xxd gdb | head -2
0000000: 7f45 4c46 0101 0100 0000 0000 0000 0000  .ELF............
0000010: 0200 2800 0100 0000 b06a 0100 3400 0000  ..(......j..4...

$ xxd gdb_pie | head -2
0000000: 7f45 4c46 0101 0100 0000 0000 0000 0000  .ELF............
0000010: 0300 2800 0100 0000 b06a 0100 3400 0000  ..(......j..4...
Save your changes and the modified binary should work well on your Android device.


