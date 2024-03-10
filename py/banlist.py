# Usage: Have script (.py file) in the same folder as your list of banned words
# name your text file inputList.txt please
# Run script and it'll create a parsed textfile in the same directory.
# Pre-condition: inputFile's words are seperated by newline character(\n)
def createLines(inputList):
    result = ''
    for word in inputList:
        result+='(?i:'+word+')\n'
    return result
naughtylist = open('inputList.txt', 'r')
badWords = naughtylist.read()
wordList = badWords.splitlines()
banListString = createLines(wordList)
# print(banListString)
newFile = open('wordsFile.txt','w+')
newFile.write(banListString)
print('Word list file created.')
newFile.close()
naughtylist.close()

