package com.thanos.chain.ledger.model.store;

import com.thanos.common.utils.rlp.RLP;
import com.thanos.common.utils.rlp.RLPList;
import com.thanos.common.utils.rlp.RLPUtil;
import com.thanos.chain.ledger.model.store.Keyable;
import com.thanos.chain.ledger.model.store.Persistable;

import java.util.ArrayList;
import java.util.List;

/**
 * TestModel.java description：
 *
 * @Author laiyiyu create on 2020-02-24 18:52:36
 */
public class TestModel extends Persistable implements Keyable {

    private String name;

    private int age;

    List<String> books;

    public TestModel(byte[] rlpEncoded) {
        super(rlpEncoded);
    }

    public TestModel(String name, int age, List<String> books) {
        super(null);
        this.name = name;
        this.age = age;
        this.books = books;
        this.rlpEncoded = rlpEncoded();
    }

    @Override
    protected byte[] rlpEncoded() {

        byte[] name = RLP.encodeString(this.name);
        byte[] age = RLP.encodeInt(this.age);

        List<byte[]> content =  new ArrayList<>();
        content.add(name);
        content.add(age);
        List<byte[]> books = rlpBooks();
        content.addAll(books);

        byte[][] elements = content.toArray(new byte[content.size()][]);
        return RLP.encodeList(elements);
    }

    private List<byte[]> rlpBooks() {
        List<byte[]> books = new ArrayList<>();

        byte[][] booksEncoded = new byte[this.books.size()][];
        int i = 0;
        for (String book: this.books) {
            booksEncoded[i] = RLP.encodeString(book);
            i++;
        }

        books.add(RLP.encodeList(booksEncoded));
        return books;
    }

    @Override
    protected void rlpDecoded() {
        RLPList params = RLP.decode2(rlpEncoded);
        RLPList test = (RLPList) params.get(0);
        this.name = RLPUtil.rlpDecodeString(test.get(0));
        this.age = RLPUtil.rlpDecodeInt(test.get(1));
        RLPList booksRLP = (RLPList) test.get(2);
        this.books = new ArrayList<>(booksRLP.size());

        for (int i = 0; i < booksRLP.size(); i++) {
            this.books.add(RLPUtil.rlpDecodeString(booksRLP.get(i)));
        }

    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public List<String> getBooks() {
        return books;
    }

    @Override
    public byte[] keyBytes() {
        return this.name.getBytes();
    }

    @Override
    public String toString() {
        return "TestModel{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", books=" + books +
                '}';
    }


    @Override
    public int compareTo(Keyable o) {
        return 0;
    }
}
