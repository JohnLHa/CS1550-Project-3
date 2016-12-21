/**
 * Page class
 */
public class Page {
    int index;
    int frame;
    int bitAge;
    int age;
    boolean valid;
    boolean dirty;
    boolean ref;

    public Page(){
        this.index = 0;
        this.frame = 0;
        this.bitAge = 0;
        this.age = 0;
        this.valid = false;
        this.dirty = false;
        this.ref = false;
    }
    public Page(Page entry){
        this.index = entry.index;
        this.frame = entry.frame;
        this.bitAge = entry.bitAge;
        this.age = entry.age;
        this.valid = entry.valid;
        this.dirty = entry.dirty;
        this.ref = entry.ref;
    }
}
