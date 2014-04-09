package jp.ad.wide.sfc.nex;

public class Range {
    int _start;
    int _end;
    public Range(int start, int end) {
	this._start = start;
	this._end = end;
    }
    public int start(){
	return this._start;
    }
    public int end(){
	return this._end;
    }
}