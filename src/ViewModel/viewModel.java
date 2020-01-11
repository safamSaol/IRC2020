package ViewModel;

import Model.*;
import Model.Merge;
import Model.ReadFile;
import com.medallia.word2vec.Word2VecModel;
import view.snowball.ext.porterStemmer;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class viewModel {
    private File subFolderTerms = null;
    private List<String> documentInQuery;
    private String postPath;

    public viewModel() {
        documentInQuery = new LinkedList<>();
    }

    /**
     * this function is the starting function of the program processes
     *
     * @param stem       if stemming is checked
     * @param postPath   the posting files path
     * @param corpusPath the corpus path
     * @return int array of the alert details
     * @throws IOException
     */

    public int[] start(boolean stem, String postPath, String corpusPath) throws IOException {

        this.postPath = postPath;
        ReadFile.setDocs(0);
        Indexer.getTermDictionary().clear();
        Indexer.getDocDictionary().clear();
        Indexer.setTotalDocLength(0);
        File[] files1 = null;
        File folder = new File(corpusPath);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        if (stem) {
            createFolders(postPath, "StemmedCorpus");
        } else {
            createFolders(postPath, "Corpus");
        }


        if (folder.isDirectory()) {
            File[] listOfSubFolders = folder.listFiles();
            List<File> files = new ArrayList<>();
            for (File SubFolder : listOfSubFolders) {
                if (SubFolder.isDirectory()) {
                    files.add(SubFolder);
                    if (files.size() == 20) {
                        ReadFile read = new ReadFile(new ArrayList<>(files), new Indexer(stem, postPath), stem, corpusPath);
                        executor.execute(new Thread(read));
                        files.clear();
                    }
                }
            }
            if (!files.isEmpty()) {
                ReadFile read = new ReadFile(new ArrayList<>(files), new Indexer(stem, postPath), stem, corpusPath);
                executor.execute(new Thread(read));
                files.clear();
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
        }

        executor = Executors.newFixedThreadPool(4);
        for (File file : subFolderTerms.listFiles()) {
            if (file.isDirectory()) {
                Merge merge = new Merge(file.listFiles());
                executor.execute(new Thread(merge));
            }
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }


        Indexer index = new Indexer(stem, postPath);

        File file = new File( postPath + "/termDictionary.txt" );
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        for (Map.Entry<String,Map<String,ArrayList<Integer>>> records : Indexer.getTermDictionary().entrySet()) {
            try{
                Integer tf = (records.getValue().get(records.getValue().keySet().toArray()[0])).get(0);
                Integer line = (records.getValue().get(records.getValue().keySet().toArray()[0])).get(1);
                writer.write(records.getKey()+">"+records.getValue().keySet().toArray()[0]+">"+(records.getValue().get(records.getValue().keySet().toArray()[0])).get(0)+">"+(records.getValue().get(records.getValue().keySet().toArray()[0])).get(1)+"\n");
            }
            catch(Exception e){
                System.out.println(records.getKey());
            }
        }
        writer.flush();
        writer.close();

        int[] corpusInfo = new int[2];
        corpusInfo[0] = index.getNumberOfTerms();
        corpusInfo[1] = ReadFile.getDocs();

        return corpusInfo;
    }

    /**
     * this function deletes all files and directories in given directory/file
     *
     * @param file the file in which we want to recursively delete all files and directories
     */
    public void delete(File file) {
        String[] lists = file.list();
        if (lists.length > 0) {
            for (String s : lists) {
                File currentFile = new File(file.getPath(), s);
                if (currentFile.isDirectory()) {
                    delete(currentFile);
                }
                currentFile.delete();
            }
        }
    }

    /**
     * this function creates all needed folders of the program
     * @param postPath posting files path
     * @param folder   folder name depending on with stemming or without
     * @throws IOException
     */

    private void createFolders(String postPath, String folder) throws IOException {
        File directory = new File(postPath + "/" + folder);
        directory.mkdir();
        subFolderTerms = new File(postPath + "/" + folder + "/Terms");
        subFolderTerms.mkdir();
        File subFolderDocs = new File(postPath + "/" + folder + "/Docs");
        subFolderDocs.mkdir();
        for (char i = 'a'; i <= 'z'; i++) {
            File Tfolder = new File(postPath + "/" + folder + "/Terms/" + i);
            Tfolder.mkdir();
            File merged = new File(subFolderTerms.getPath() + "/" + i, i + "_merged.txt");
            merged.createNewFile();
        }
        File Sfolder = new File(subFolderTerms.getPath() + "/special");
        Sfolder.mkdir();
        File merged = new File(subFolderTerms.getPath() + "/special", "special" + "_merged.txt");
        merged.createNewFile();
        //File mergedDoc = new File(subFolderDocs.getPath() + "/docDictionary", "docDictionary" + "_merged.txt");
        //mergedDoc.createNewFile();
    }

    /**
     * this function display the dictionary generated from the program
     *
     * @param selected stemming selected/not selected
     * @param postPath posting files path
     * @return the String to be displayed
     * @throws IOException
     */

    public LinkedList<String> displayDictionary(boolean selected, String postPath) throws IOException {
        //String dictionary = "";
        Indexer index = new Indexer(selected, postPath);
        Set<String> termsKey = index.getTermDictionary().keySet();
        LinkedList<String> dictionary = new LinkedList<>();
        if (termsKey.size() > 0) {
            for (String term : termsKey) {
                String currTerm = term;
                dictionary.add("The TF for : " + currTerm + " -> " + index.getTermDictionary().get(term).get(index.getTermDictionary().get(term).keySet().toArray()[0]));
                //dictionary = dictionary + currTerm + "\n";
            }
            return dictionary;
        } else {
            return null;
        }
    }

    /**
     * this function loads dictionary from the hard disk to RAM
     *
     * @param selected stemming selected/not selected
     * @param postPath posting file path
     * @throws IOException
     */

    public void loadDictionary(boolean selected, String postPath) throws IOException {
        File file = new File(postPath + "/termDictionary.txt");
        Indexer index = new Indexer(selected, postPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String st;
        Map<String, Map<String, ArrayList<Integer>>> termDictionary = new TreeMap<>();
        index.clearMap();
        while ((st = br.readLine()) != null) {
            String[] term = st.split(">");
            if (term.length == 4) {
                termDictionary.put(term[0], new HashMap<>());
                termDictionary.get(term[0]).put(term[1], new ArrayList<>());
                termDictionary.get(term[0]).get(term[1]).add(0, Integer.parseInt(term[2]));
                termDictionary.get(term[0]).get(term[1]).add(1, Integer.parseInt(term[3]));
            }
        }
        index.setTermDictionary(termDictionary);
    }

    public LinkedList<String> startQuery(String path, String stopWordsPath, boolean stem, boolean semanticSelected,boolean isDescription) throws IOException, ParseException, InterruptedException {
        Searcher searcher = new Searcher(path, stopWordsPath, stem,isDescription);
        List<Query> queryList = searcher.readQuery();
        Map<String,Map<String,Double>> docsRanks = new HashMap<>();
        for(Query query: queryList){
            docsRanks.put(query.getNumOfQuery(),getAllRankedDocs(query,semanticSelected, stem, isDescription));
        }

        writeToResultFile(docsRanks);

        return displayQueries(docsRanks);
    }

    private LinkedList<String> displayQueries(Map<String, Map<String, Double>> docsRanks) {
        LinkedList<String> display = new LinkedList<>();
        int num = 0;
        for (String s: docsRanks.keySet()) {
            String str = "";
            display.add("For query number " + s + " the most fifty or less documents are:"+"\n");
            for (String docStr: topFifty(docsRanks.get(s)).keySet()) {
                documentInQuery.add(docStr);
                str = str + "," + docStr;
                num++;
                if(str.split(",").length%10==0){
                    display.add(str);
                    str = "";
                }
            }
            display.add(str);
        }
        display.addFirst("Total number of queries are :" + docsRanks.keySet().size() +"\n"
                +"Total document returned for all the queries are :" + num);
        return display;
    }

    public LinkedList<String> startSingleQuery(String query, String stopWordsPath, boolean stem, boolean semanticSelected,boolean isDescription) throws IOException, ParseException, InterruptedException {
        Searcher searcher = new Searcher(query, stopWordsPath, stem,isDescription);
        Query singleQuery = searcher.startSingleQuery();

        Map<String,Map<String,Double>> docsRanks = new HashMap<>();
        docsRanks.put(singleQuery.getNumOfQuery(),getAllRankedDocs(singleQuery,semanticSelected,stem,isDescription));

        writeToResultFile(docsRanks);

        return displayQueries(docsRanks);
    }

    private void writeToResultFile(Map<String, Map<String, Double>> docsRanks) throws IOException {

        File file = new File( postPath + "/results.txt" );
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        for(Map.Entry<String, Map<String, Double>> entry: docsRanks.entrySet()){
            for(Map.Entry<String, Double> docEntry: entry.getValue().entrySet()){
                writer.write(entry.getKey() + " " + "0"+ " " +docEntry.getKey() +" "+docEntry.getValue() +" "+ "42.38" + " " + "i&i"+'\n');
            }
        }
        writer.flush();
        writer.close();
    }

    public List<String> getDocumentInQuery() {
        return documentInQuery;
    }

    private Map<String, Double> getAllRankedDocs(Query queriesTokens, boolean semanticSelected, boolean stem, boolean isDescription) throws IOException {
        List<String> queryWithSemantic = new ArrayList<>();

        //intersaction with terms of inverted index
        Set indexedTerms = Indexer.getTermDictionary().keySet();
        //todo change to end of semantic query ;

        Set<String> retrievedDocs = new HashSet<>();

        Map<String,Double> docsRanks = new HashMap<>();

        try{
            Word2VecModel model = Word2VecModel.fromTextFile(new File("resources/word2vec.c.output.model.txt"));
            com.medallia.word2vec.Searcher semanticSearcher = model.forSearch();

            int numOfResults = 10;

            if(semanticSelected){
                getRelevantDocsWithSemantics(queriesTokens,queryWithSemantic,indexedTerms,semanticSearcher,numOfResults,stem,isDescription);
            }

            List<String> queryToRank = queriesTokens.getTokenQuery();
            if(stem){
                List<String> stemmedQuery = new ArrayList<>();
                for(String term: queryToRank){
                    stemmedQuery.add(stemTerm(term));
                }
                queryToRank = stemmedQuery;
            }
            getRelevantDocs(queryToRank,retrievedDocs);
            queryToRank.retainAll(indexedTerms);
            queryWithSemantic.retainAll(indexedTerms);

            docsRanks = rankDocs(retrievedDocs,queryToRank,queryWithSemantic);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return docsRanks;

    }

    private void getRelevantDocs(List<String> queryToRank, Set<String> retrievedDocs) {

        for (String queryTerm : queryToRank) {
            addDocstoRetrievedDocs(queryTerm,retrievedDocs);
        }
    }

    private void getRelevantDocsWithSemantics(Query queryToRank, List<String> queryWithSemantic, Set indexedTerms, com.medallia.word2vec.Searcher semanticSearcher, int numOfResults, boolean stem, boolean isDescription) {
        for (String queryTerm : queryToRank.getTokenQuery()) {
            try {

                List<com.medallia.word2vec.Searcher.Match> matches = semanticSearcher.getMatches(queryTerm, numOfResults);
                for (com.medallia.word2vec.Searcher.Match match : matches) {

                    String semanticTerm =match.match();
                    if(stem){
                        semanticTerm = stemTerm(semanticTerm);

                    }
                    //todo insert to if queryToRank.getDesc().contains(semanticTerm) and increase numOfResults
                    if((indexedTerms.contains(semanticTerm) || indexedTerms.contains(semanticTerm.toLowerCase()) || indexedTerms.contains(semanticTerm.toUpperCase())) && !queryTerm.contains(semanticTerm)){
                        queryWithSemantic.add(semanticTerm.toLowerCase());
                        queryWithSemantic.add(semanticTerm.toUpperCase());
                        queryWithSemantic.add(semanticTerm);
                        //addDocstoRetrievedDocs(semanticTerm,retrievedDocsWithSemantics);
                    }
                    if(isDescription){
                        ArrayList<String> descSemantic = new ArrayList<>();
                        ArrayList<String> descSet = queryToRank.getTokenDesc();
                        for(String desc: descSet){
                            if(stem){
                                desc = stemTerm(desc);
                            }
                            descSemantic.add(desc);
                            descSemantic.add(desc.toLowerCase());
                            descSemantic.add(desc.toUpperCase());
                        }
                        queryWithSemantic.addAll(descSemantic);
                    }

                }
            }catch (com.medallia.word2vec.Searcher.UnknownWordException e) {
                // TERM NOT KNOWN TO MODEL
            }
        }
    }

    private String stemTerm(String semanticTerm) {
        porterStemmer ps = new porterStemmer();
        ps.setCurrent(semanticTerm);
        ps.stem();
        return ps.getCurrent();
    }


    private Map<String, Double> rankDocs(Set<String> retrievedDocs, List<String> queryToRank, List<String> queryWithSemantic) {
        Ranker ranker = new Ranker();
        Map<String,Double> docsRanks = new HashMap<>();
        for(String doc: retrievedDocs){
            double originalRank = ranker.score(queryToRank,doc);
            double newRank = 0.9*originalRank +0.1*ranker.score(queryWithSemantic,doc);
            docsRanks.put(doc,newRank);

        }

        return docsRanks;
    }

    private void addDocstoRetrievedDocs(String term, Set<String> retrievedDocs) {
        List<String> postingLine = Ranker.getPostingLine(term);
        for (String str : postingLine) {
            String[] termInfo = str.split("\\|");
            retrievedDocs.add(termInfo[0]);
        }
    }

    private Map<String,Double> topFifty(Map<String,Double> docRanked){
        Map<String,Double> docRankCopy = new HashMap<>(docRanked);
        if(docRanked.size()>50) {
            int numberOfdocs = 0;
            Map<String,Double> topFifty = new HashMap<>();
            while (numberOfdocs!=50) {
                //int max = entitiesPerDoc.get(0);
                Set<String> str = docRankCopy.keySet();
                String [] strArr = new String[docRankCopy.keySet().size()];
                strArr = str.toArray(strArr);
                double max = docRankCopy.get(strArr[0]);
                String maxString  =strArr[0];
                for (int k = 1; k < strArr.length; k++) {
                    if (docRankCopy.get(strArr[k])>max) {
                        max = docRankCopy.get(strArr[k]);
                        maxString = strArr[k];
                    }
                }
                docRankCopy.remove(maxString);
                topFifty.put(maxString,max);
                numberOfdocs++;
            }
            return topFifty;
        }else if(docRankCopy.keySet().size()>=0){
            return docRanked;
        }
        return null;
    }
}
