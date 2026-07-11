package com.smartchunker.store.impl;

import com.smartchunker.store.VectorStore;
import com.smartchunker.store.model.SearchHit;
import com.smartchunker.store.model.VectorDoc;

import java.util.*;

public class HnswVectorStore implements VectorStore {

    private final int m;
    private final int mMax;
    private final int mMax0;
    private final int efConstruction;
    private final int efSearch;

    private final Map<String, Integer> idToIndex = new HashMap<>();
    private final List<HnswNode> nodes = new ArrayList<>();
    private final Map<Integer, Map<String, String>> metadataMap = new HashMap<>();

    private int entryPoint = -1;
    private int maxLevel = -1;
    private final Set<Integer> deletedIndices = new HashSet<>();
    private final Random random = new Random();

    private static final float ML = 1.0f / (float) Math.log(2.0);

    public HnswVectorStore() {
        this(16, 200, 100);
    }

    public HnswVectorStore(int m, int efConstruction, int efSearch) {
        this.m = m;
        this.mMax = m;
        this.mMax0 = m * 2;
        this.efConstruction = efConstruction;
        this.efSearch = efSearch;
    }

    @Override
    public void add(String id, float[] vector, Map<String, String> metadata) {
        add(new VectorDoc(id, vector, metadata));
    }

    @Override
    public void add(VectorDoc doc) {
        if (idToIndex.containsKey(doc.getId())) {
            delete(doc.getId());
        }

        int idx = nodes.size();
        idToIndex.put(doc.getId(), idx);
        metadataMap.put(idx, doc.getMetadata() != null
                ? new HashMap<>(doc.getMetadata()) : new HashMap<>());

        int level = randomLevel();
        HnswNode node = new HnswNode(doc.getVector(), level);
        nodes.add(node);

        if (entryPoint == -1) {
            entryPoint = idx;
            maxLevel = level;
            return;
        }

        int ep = entryPoint;
        int l = maxLevel;

        for (int lc = l; lc > level; lc--) {
            if (nodes.get(ep).neighbors.length <= lc) continue;
            ep = searchLayer(doc.getVector(), ep, 1, lc).get(0);
        }

        for (int lc = Math.min(level, l); lc >= 0; lc--) {
            int ef = efConstruction;
            List<Integer> candidates = searchLayer(doc.getVector(), ep, ef, lc);
            List<Integer> selected = selectNeighbors(doc.getVector(), candidates,
                    lc == 0 ? mMax0 : mMax);
            connectNode(idx, selected, lc);
            ep = candidates.get(0);
        }

        if (level > maxLevel) {
            maxLevel = level;
            entryPoint = idx;
        }
    }

    @Override
    public List<SearchHit> search(float[] queryVector, int topK) {
        if (nodes.isEmpty() || idToIndex.isEmpty()) {
            return Collections.emptyList();
        }

        int ep = findValidEntryPoint();
        if (ep == -1) {
            return Collections.emptyList();
        }

        int l = maxLevel;
        int ef = Math.max(efSearch, topK);

        for (int lc = l; lc > 0; lc--) {
            if (nodes.get(ep).neighbors.length <= lc) continue;
            List<Integer> layerResult = searchLayer(queryVector, ep, 1, lc);
            if (!layerResult.isEmpty()) {
                ep = layerResult.get(0);
            }
        }

        List<Integer> candidates = searchLayer(queryVector, ep, ef, 0);

        List<SearchHit> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, candidates.size()); i++) {
            int idx = candidates.get(i);
            if (deletedIndices.contains(idx)) continue;
            HnswNode node = nodes.get(idx);
            float score = cosineSimilarity(queryVector, node.vector);
            String id = findIdByIndex(idx);
            if (id == null) continue;
            Map<String, String> meta = metadataMap.getOrDefault(idx, Collections.emptyMap());
            results.add(new SearchHit(id, score, meta));
        }

        return results;
    }

    private int findValidEntryPoint() {
        if (entryPoint >= 0 && !deletedIndices.contains(entryPoint)) {
            return entryPoint;
        }
        for (int i = 0; i < nodes.size(); i++) {
            if (!deletedIndices.contains(i)) {
                entryPoint = i;
                return i;
            }
        }
        return -1;
    }

    @Override
    public void delete(String id) {
        Integer idx = idToIndex.remove(id);
        if (idx == null) return;

        deletedIndices.add(idx);
        metadataMap.remove(idx);

        for (int lc = 0; lc <= maxLevel && lc < nodes.get(idx).neighbors.length; lc++) {
            List<Integer> neighbors = nodes.get(idx).neighbors[lc];
            for (int neighbor : neighbors) {
                if (lc < nodes.get(neighbor).neighbors.length) {
                    nodes.get(neighbor).neighbors[lc].remove(Integer.valueOf(idx));
                }
            }
        }
    }

    @Override
    public int size() {
        return idToIndex.size();
    }

    @Override
    public void clear() {
        idToIndex.clear();
        nodes.clear();
        metadataMap.clear();
        deletedIndices.clear();
        entryPoint = -1;
        maxLevel = -1;
    }

    @Override
    public List<VectorDoc> getAll() {
        List<VectorDoc> docs = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : idToIndex.entrySet()) {
            HnswNode node = nodes.get(entry.getValue());
            Map<String, String> meta = metadataMap.get(entry.getValue());
            docs.add(new VectorDoc(entry.getKey(), node.vector, meta));
        }
        return docs;
    }

    private int randomLevel() {
        double r = -Math.log(1.0 - random.nextDouble()) * ML;
        return (int) r;
    }

    private List<Integer> searchLayer(float[] query, int ep, int ef, int level) {
        Set<Integer> visited = new HashSet<>();
        PriorityQueue<Candidate> candidates = new PriorityQueue<>(
                Comparator.comparingDouble(c -> c.distance));
        PriorityQueue<Candidate> results = new PriorityQueue<>(
                Comparator.comparingDouble(c -> -c.distance));

        float dist = 1.0f - cosineSimilarity(query, nodes.get(ep).vector);
        candidates.add(new Candidate(ep, dist));
        results.add(new Candidate(ep, dist));
        visited.add(ep);

        while (!candidates.isEmpty()) {
            Candidate c = candidates.poll();
            Candidate worst = results.peek();

            if (results.size() >= ef && c.distance > worst.distance) {
                break;
            }

            List<Integer> neighbors = nodes.get(c.index).neighbors[level];
            for (int neighbor : neighbors) {
                if (visited.contains(neighbor) || deletedIndices.contains(neighbor)) continue;
                if (nodes.get(neighbor).neighbors.length <= level) continue;
                visited.add(neighbor);

                float d = 1.0f - cosineSimilarity(query, nodes.get(neighbor).vector);
                Candidate worstResult = results.peek();

                if (d < worstResult.distance || results.size() < ef) {
                    candidates.add(new Candidate(neighbor, d));
                    results.add(new Candidate(neighbor, d));

                    if (results.size() > ef) {
                        results.poll();
                    }
                }
            }
        }

        List<Integer> resultList = new ArrayList<>();
        while (!results.isEmpty()) {
            resultList.add(results.poll().index);
        }
        Collections.reverse(resultList);
        return resultList;
    }

    private List<Integer> selectNeighbors(float[] query, List<Integer> candidates, int m) {
        List<Integer> result = new ArrayList<>();
        PriorityQueue<Candidate> heap = new PriorityQueue<>(
                Comparator.comparingDouble(c -> -c.distance));

        for (int c : candidates) {
            float dist = 1.0f - cosineSimilarity(query, nodes.get(c).vector);
            if (heap.size() < m) {
                heap.add(new Candidate(c, dist));
                result.add(c);
            } else if (dist < heap.peek().distance) {
                Candidate removed = heap.poll();
                result.remove(Integer.valueOf(removed.index));
                heap.add(new Candidate(c, dist));
                result.add(c);
            }
        }

        return result;
    }

    private void connectNode(int src, List<Integer> neighbors, int level) {
        HnswNode srcNode = nodes.get(src);
        for (int dst : neighbors) {
            if (dst == src) continue;
            addEdge(srcNode, dst, level);
            addEdge(nodes.get(dst), src, level);
        }
    }

    private void addEdge(HnswNode node, int target, int level) {
        List<Integer> neighborList = node.neighbors[level];
        int maxEdges = (level == 0) ? mMax0 : mMax;

        if (neighborList.contains(target)) return;

        if (neighborList.size() < maxEdges) {
            neighborList.add(target);
        } else {
            float[] nodeVec = node.vector;
            float[] targetVec = nodes.get(target).vector;
            float targetDist = 1.0f - cosineSimilarity(nodeVec, targetVec);

            int worstIdx = 0;
            float worstDist = -1;
            for (int i = 0; i < neighborList.size(); i++) {
                float d = 1.0f - cosineSimilarity(nodeVec, nodes.get(neighborList.get(i)).vector);
                if (d > worstDist) {
                    worstDist = d;
                    worstIdx = i;
                }
            }

            if (targetDist < worstDist) {
                neighborList.set(worstIdx, target);
            }
        }
    }

    private String findIdByIndex(int idx) {
        for (Map.Entry<String, Integer> entry : idToIndex.entrySet()) {
            if (entry.getValue() == idx) {
                return entry.getKey();
            }
        }
        return null;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return (normA == 0 || normB == 0) ? 0 : dot / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private static class HnswNode {
        final float[] vector;
        final List<Integer>[] neighbors;

        @SuppressWarnings("unchecked")
        HnswNode(float[] vector, int level) {
            this.vector = vector;
            this.neighbors = new List[level + 1];
            for (int i = 0; i <= level; i++) {
                this.neighbors[i] = new ArrayList<>();
            }
        }
    }

    private static class Candidate {
        final int index;
        final float distance;

        Candidate(int index, float distance) {
            this.index = index;
            this.distance = distance;
        }
    }
}