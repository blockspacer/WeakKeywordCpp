class A {
};

class _B : public A {};

class B : public _B {
public:
    B() {}
    virtual void f() {}
};

int main() {
    A* a = new A;
    B* b = static_cast<_B*>(a);
//    b->f();
}
