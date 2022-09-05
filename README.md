## THIS PROJECT IS NO LONGER BEING ACTIVELY WORKED ON. A NEW FUTURE COP PARSER IS NOW IN THE WORKS IN C#.
The reasoning for this is because I've decided to move away from JavaFX for the FCEditor in favor of Unity for the 3D map maker.
https://github.com/BahKooJ/FCopParseLibSharp

# Fcop-Parse-Lib
## A Kotlin library for working with Future Cop's mission files
This library parses Future Cop's IFF Files and the data contained inside it, turning them into readable objects.
Note that this only works for the PC version (PS support coming later)

## IffFile and FCopMissionFile
The IffFile object is what parses the IFF file and gets all the necessary data need to work with them. However they remain in there original state unchanged. Making them not very readable or easy to use. 
Which is where the FCopMissionFile object comes in. This object completely uncompresses the file making all the data individual FCopData objects. For easy of modification. However this object is not perfect yet, and still has some issue that need to be ironed out. If you encounter something use the IffFile object instead.

## FCopData
This object is for storing the contents of a file, objects derive from FCopData for further parsing and usage. 
