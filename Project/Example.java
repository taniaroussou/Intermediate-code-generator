

class Example {
    public static void main(String[] args){
        A a;
        int sum;
        int i;
        int[] data;
        int dummy;

        i = 0;
        sum = 0;
        a = new A();
        dummy = a.init(100);
        data = a.get_data();
        while(i < 100) {
            // sum = sum + i;
            // sum = i;
            dummy = a.set(i, i);
            i = i + 1;
        }

        i = 0;
        while (i < 50) {
            dummy = a.set_sign(i < 25, i);
            i = i + 1;
        }

        while (i < 100) {
            dummy = a.set_sign(i < 75, i);
            i = i + 1;
        }

        i = 0;
        while (i < 100){
            System.out.println(data[i]);
            i = i + 1;
        }
    }
}

class A {
    int[] a;
    int l;

  
    public int init(int _l){
        a = new int[_l];
        l = _l;
        return 0;
    }

    public int reset_all(){
        int i;
        i = 0;
        while(i < l) {
            a[i] = 0;
            i = i + 1;
        }
        return 0;
    }

    public int reset(int idx){
        a[idx] = 0;
        return 0;
    }

    public int set_all(int val){
        int i;
        i = 0;
        while(i < l) {
            a[i] = val;
            i = i +1;
        }

        return 0;
    }

    public int set(int idx, int val){
        a[idx] = val;
        return 0;
    }

    public int[] get_data(){
        return a;
    }

    public int get(int idx){
        return a[idx];
    }


    public int set_sign(boolean sign, int idx){
        int p;
        int test;
        test = 0;
        test = test - 1;
        p = a[idx];
        if (sign) {
            if ( p < 0) {

                a[idx] = p * test;
            }
            else {
             
            }
        }
        else {
            if ( !(p < 0)) {

                a[idx] = p * test;     
            }
            else {
                p = 0;
            }
        }
          
        return 0;
    }
}
