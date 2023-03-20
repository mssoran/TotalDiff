# TODO
Example:
to run: `bazel run totaldiff:totaldiff`

# Build Deployment Jar

```
bazel build totaldiff:totaldiff_deploy.jar
```

Then you can copy `bazel-bin/totaldiff/totaldiff_build.jar` or run it as:

```
java -jar bazel-bin/totaldiff/totaldiff_deploy.jar
```

# Arguments and Run Options

The first argument determines the running mode. Supported modes:
- buildindex: Builds an index file to use. Usually time consuming for large disks.
- analyze: Find duplicates and print/visualize in a useful way.
- diff: Show difference of two index files.

## buildindex
Collects data and construct info tree.

```
    buildindex: Builds an index file to use for the other commands. It recursively crawls the
                given directories, and collects information for each file.
       --infoTreeInputFileName=<input file name>
                Name of the input file, that's previously generated with buildindex command.
                Files in this input is used as the basis. If the index building is incremental, 
                all files in the input are used and new files are added from new dirs. If index
                building is not incremental, files in the input are used as lookup for expensive
                calculations, like hashing. Instead of hashing again, value in the input is used.
                Can be set empty, so no input is used. Default value is empty.
       --dirs=<dir list>
                List of directories to scan. All the files under each of these directories will be
                included in the generated output file recursively. It's a list of directory names
                separated by ':' character. Default value is empty
       --[no]incrementalAddDirs
                Determines how to use infoTreeInputFileName. If true, uses all the files in the 
                input file, and adds any other new file it finds in the directories scanned. If
                false, uses input file only as a lookup for hash values. The output only contains
                files from scanned directories.
                incrementalAddDirs is useful when adding new directories to an existing built file
                noincrementalAddDirs is useful when re-generating build file. In this case, deleted
                files will not be in the new output, not changed files will be used from the input
                and only new files will be hashed.
       --infoTreeOutputFileName=<output file name>
                Index file generated. All the found files will be written to this output.
                Can be set empty, so no input is used. Default value is empty.
       --[no]overwriteInfoTreeOutputFile
                If true, overwrites the output file when it exists. If false, fails with an error
                when output file already exists. Default value is false.
       --[no]compareHashes
                A boolean option. If true, uses file content for comparison. Default value is
                true. For efficiency, only part of the content is used.
                See --maxSizeForHashComparison.
       --maxSizeForHashComparison=<size in bytes>
                When calculating hash of a file, at most maxSizeForHashComparison bytes from the
                beginning of the file is used. Default value is 8 MB
       --fileReadBufferSize=<size in bytes>
                Size of the buffer when reading a file. Default value is 128 KB. It's used during
                hash calculation.
       --considerFileSize=<size in bytes>
                If size of a file is less than considerFileSize, the file is skipped, and it will
                not be included in the index (or any computation). Default value is 0. This option
                can be used to exclude small files.
       --considerDirExcludes=<dir list>
                This option can bu used to exclude directories/files with a specific name. It's a
                list of names separated by ':' character. Default value contains some dirs like
                .git
```


## analyze 
This stage is analyzing data, finding duplicate files, and generating graph. 

```
    analyze: Analyzes the given input file (previously generated file with buildindex) and
             generates corresponding output. *Details are still in progress*. Currently generates
             a graph for the top printTopCount duplicated directories.
       --infoTreeInputFileName=<input file name>
             Data in this file is used for the analyze. See definition in buildindex
       --graphOutputFile=<output file name>
             Name of the output file for the generated graph.
       --considerFileSize=<size in bytes>
             See definition in buildindex
       --considerDirExcludes=<dir list>
             See definition in buildindex
       --[no]printDuplicates
             If true, outputs a human readable list of all the duplicated files found. Default
             value is false.
       --[no]includePathInGraph
             Work in progress.
       --[no]ignorePrintingNodesInSinglePath
             Work in progress.
       --printTopCount=<count>
             Generate the graph using the largest <count> duplicate groups.
```
## diff
Compares two input files.

```
    diff:    Compares two given input files, and prints all the files in the first input, but not
             in the second input.
       --infoTreeInputFileName=<input file name>
             Data in this file is used for the diff as the first input. See definition in buildindex
       --infoTreeSecondaryInputFileName=<input file name>
             Data in this file is used for the diff as the second input. Similar to
             infoTreeInputFileName
       --considerFileSize=<size in bytes>
             See definition in buildindex
       --considerDirExcludes=<dir list>
             See definition in buildindex
```

## Best practice to run:
1) Collect data by giving only `dirs`, not `infoTreeInputFileName`, and write the result to an output file:

```
java -jar totaldiff_deploy.jar buildindex --infoTreeInputFileName= --dirs=dir1:dir2:dir3 --infoTreeOutputFileName=outputfilename
bazel build totaldiff:totaldiff_deploy.jar && java -jar bazel-bin/totaldiff/totaldiff_deploy.jar buildindex --infoTreeInputFileName= --dirs=~/test --infoTreeOutputFileName=withdigestoutputtest.txt
```

2) If the processed data is very large, and it takes long time, it's more prone to failures during collection.
If for any reason, collection fails in the middle of the process, it's possible to continue the data 
collection. To do so, first move the previous `outputfilename` output file as `inputfilename`, and feed
it to the next run as an input file

```
java -jar totaldiff_deploy.jar buildindex --infoTreeInputFileName=inputfilename --dirs=dir1:dir2:dir3 --infoTreeOutputFileName=outputfilename
bazel build totaldiff:totaldiff_deploy.jar && java -jar bazel-bin/totaldiff/totaldiff_deploy.jar buildindex --infoTreeInputFileName=withdigestinputtest.txt --dirs=/Users/serdar/test --infoTreeOutputFileName=withdigestoutputtest.txt
```

3) Analyze the data and create the output

```
java -jar totaldiff_deploy.jar analyze --infoTreeInputFileName=previouslygeneratedfile
bazel build totaldiff:totaldiff_deploy.jar && java -jar bazel-bin/totaldiff/totaldiff_deploy.jar analyze --infoTreeInputFileName=withdigestoutputtest.txt

```

4) Then, you can visualize the generated graph

```
dot -Tsvg ldgraphOut.dot > hoho2.svg && open hoho2.svg
```

5) If you want to see a diff of two input files:
```
java -jar totaldiff_deploy.jar diff --infoTreeInputFileName=previouslygeneratedfile --infoTreeSecondaryInputFileName=filterFromInputFile
bazel build totaldiff:totaldiff_deploy.jar && java -jar bazel-bin/totaldiff/totaldiff_deploy.jar diff --infoTreeInputFileName=totalDiffYeniambarOutput.txt --infoTreeSecondaryInputFileName=totalDiffYeniambarOutputSiyahWD.txt
```

6) If You add/remove some of the files, and want to buildindex again, you can do it much faster by giving the previous output file as a lookup file. You can do it by setting `noincrementalAddDirs`

```
java -jar totaldiff_deploy.jar buildindex --infoTreeInputFileName=inputfilename --dirs=dir1:dir2:dir3 --noincrementalAddDirs --infoTreeOutputFileName=outputfilename
bazel build totaldiff:totaldiff_deploy.jar && java -jar bazel-bin/totaldiff/totaldiff_deploy.jar buildindex --infoTreeInputFileName=withdigestinputtest.txt --dirs=/Users/serdar/test --noincrementalAddDirs --infoTreeOutputFileName=withdigestoutputtest.txt
```

Then, you can analyze again as above

# TODO

- [x] Change the command format: `command --option1 --option2` . Because, different commands may need 
different options and limitations.
- [x] Add support for incremental update of output file. It can work like: read the last file, list all files in all directories, if a file is not in list (how to decide? I's possible that the content is changed, but the name is not. Best way may be keeping the last change time. But it may be ok for now to look at size only.), only then hash the file and add to the list. If a file is in list but not on the disk, remove it from the list. This way it'll be faster during the second run. 
- [x] Change the format of output file into json

