package com.example.jpiet;

import java.util.ArrayList;
import java.util.List;

public class CodelTableModelScanerIterative implements CodelTableModelScaner {
    
    private final int DELTA_DOWN = 1;
    private final int DELTA_UP = -1;
    
    private class Segment{
        ArrayList<CodelColor> data;
        ArrayList<Area> parents;
        int x;
        int y;
        CodelColor value;
        
        public Segment(int x, int y, CodelColor kind){
            this.x = x;
            this.y = y;
            value = kind;
            data = new ArrayList<CodelColor>();
            data.add(kind);
            parents = new ArrayList<Area>();
        }
        
        public boolean hasValueAt(int index){
            if (index < this.x || index > this.getLastIndex()) {
                return false;
            }
            
            return true;
        }
        
        public int size(){
            return data.size();
        }
        
        public int getLastIndex(){
            return this.x + size();
        }
        
        public boolean isValidValue(CodelColor value){
            return this.value == value;
        }
        
        public void add(CodelColor value) {
            this.data.add(value);
        }
        
        public void addParent(Area parent) {
            this.parents.add(parent);
        }
        
        public boolean hasParents() {
            return this.parents.size() != 0;
        }
        
        public int getCountOfParents() {
            return this.parents.size();
        }
        
        public String toString() {
            return String.format("Segment on row %i from %i to %i with data %s" 
                    , this.y, this.x, this.getLastIndex(), this.data.toString());
        }

        public List<Area> getParents() {
            return parents;
        }

        public boolean hasManyParents() {
            return parents.size() > 1;
        }
    }
    
    private class Area{
        ArrayList<Segment> segments;
        boolean touched;
        
        public Area(Segment first) {
            segments = new ArrayList<Segment>();
            first.addParent(this);
            this.segments.add(first);
            
            touched = false;
        }
        
        public void untouch() {
            touched = false;
        }
        
        public void tryToMerge(Segment segment, int delta) {
            ArrayList<Segment> segments = new ArrayList<Segment>();
            getSegmentsAtRow(segment.y - delta, segments);
            if (segments.size() == 0) {
                return;
            }
            
            for (Segment existedSegment : segments) {
                if (isIntersected(existedSegment, segment)) {
                    this.add(segment);
                }
            }
        }
        
        public void merge(Area area) {
            for(Segment segment : area.segments) {
                this.add(segment);
            }
        }
        
        public void add(Segment segment) {
            this.touched = true;
            segment.addParent(this);
            this.segments.add(segment);
        }
        
        public void getSegmentsAtRow(int rowIndex, ArrayList<Segment> destination) {
            //FIX ME
            for (Segment segment : segments) {
                if (segment.y == rowIndex) {
                    destination.add(segment);
                }
            }
        }
        
        public boolean isIntersected(Segment segment1, Segment segment2) {
            if (segment1.value != segment2.value){
                return false;
            }
            int last = segment1.getLastIndex();
            for (int i = segment1.x; i < last; i++) {
                if (segment2.hasValueAt(i)) {
                    return true;
                }
            }
           
            return false;
        }
        
        @Override
        public String toString() {
            String repr = "";
            for (Segment segment : segments) {
                repr += segment + "\n";
            }
            
            return repr;
        }

        public boolean isTouched() {
            return touched;
        }

        public List<Segment> getSegments() {
            return segments;
        }
    }
    
    private List<Segment> getSegmentsAtRow(CodelTableModel model
            , int index){
        List<CodelColor> row = model.getRow(index);
        int size = row.size();
        if (size == 0) {
            return null;
        }
        
        List<Segment> segments = new ArrayList<Segment>();
        CodelColor first = row.get(0);
        
        Segment segment = new Segment(0, index, first);
        
        for (int i = 1; i < size; i++) {
            
            CodelColor value = row.get(i);
            
            if (segment.isValidValue(value)) {
                segment.add(value);
            }
            else {
                segments.add(segment);
                segment = new Segment(i, index, value);
            }
        }
        
        if (size > 1){
            segments.add(segment);
        }
        
        return segments;
    }
    
    private void mergeSegmentAreas(List<Area> areas, Segment segment) {
        List<Area> parents = segment.getParents();
        Area big_parent = parents.get(0);
        int size = parents.size();
        for (int i = 1; i < size; ++i) {
            Area soonDiedParent = parents.get(i);
            big_parent.merge(soonDiedParent);
            areas.remove(soonDiedParent);
        }     
    }
    
    private Area searchConnectedAreas(CodelTableModel model, int x, int y) {
        List<Segment> segments = getSegmentsAtRow(model, y);
        if(segments == null) {
            return null;
        }
        
        List<Area> areas = new ArrayList<Area>();
        Area result = null;
        
        if (x == 6 && y == 0) {
            int bdsm = 1;
            bdsm += 1;
            int v = bdsm;
        }
        for (Segment segment : segments) {
            Area area = new Area(segment);
            if (segment.hasValueAt(x)) {
                result = area;
            }
            
            areas.add(area);
        }
        
        if (result == null) {
            return null;
        }
        
        fillArea(areas, result, model, y - 1, -1, DELTA_UP);
        fillArea(areas, result, model, y + 1
                , model.getHeight(), DELTA_DOWN);
        
        return result;
    }
    
    private void fillArea(List<Area> areas, Area result,
            CodelTableModel model, int first, int last, int delta) {
        
        for (int i = first; i != last; i += delta) {
            for (Area area : areas) {
                area.untouch();
            }
            
            List<Segment> segments = getSegmentsAtRow(model, i);
            if (segments == null) {
                continue;
            }
            
            for (Segment segment: segments) {
                for(Area area : areas) {
                    area.tryToMerge(segment, delta);
                }
                
                if (segment.hasParents() == false) {
                    Area area = new Area(segment);
                    areas.add(area);
                }
                
                if (segment.hasManyParents()) {
                    mergeSegmentAreas(areas, segment);
                }        
            }
            
            for(Area area : areas) {
                if(area.isTouched() == false && area.equals(result)) {
                    return;
                }
            }
        }
    }
    
    private CodelTableModel mModel;
    protected CodelArea mArea;
    
    CodelTableModelScanerIterative() {
        mArea = new CodelArea();
    }
    
    @Override
    public CodelArea getCodelArea() {
        return mArea;
    }
    
    @Override
    public void setModel(CodelTableModel _model) {
        mModel = _model;
    }

    @Override
    public void scanForCodelNeighbors(int x, int y) {
        int width = mModel.getWidth();
        int height = mModel.getHeight();
        

        CodelColor value = mModel.getValue(x, y);

        mArea.init(x, y, value);
        mArea.setDebugRestriction(width, height);
        
        Area area = searchConnectedAreas(mModel, x, y);
        
        if(area == null) {
            @SuppressWarnings("unused")
            int bdsm = 1;
            
        }
        
        List<Segment> segments = area.getSegments();
       
        for (Segment segment : segments) {
            int last = segment.getLastIndex();
            int first = segment.x;
            for(int i = first; i < last; ++i) {
                //FIXME
                if(x == i && y == segment.y){
                    continue;
                }
                mArea.add(i, segment.y);
            }
        }
    }

}
