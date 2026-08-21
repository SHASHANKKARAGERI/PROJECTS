import java.io.*;
import java.nio.*;
import java.util.*;
 
// ── Student class with pack() and unpack() methods ───────────────────────────
class Student implements Serializable {
 
    private static final long serialVersionUID = 1L;
 
    // Fixed sizes for ByteBuffer layout: 40 + 40 + 4 + 8 = 92 bytes per record
    static final int NAME_LEN   = 40;
    static final int EMAIL_LEN  = 40;
    static final int RECORD_LEN = NAME_LEN + EMAIL_LEN + Integer.BYTES + Double.BYTES;
 
    private String name;
    private int    rollNo;
    private double cgpa;
    private String email;
 
    Student() {}
    Student(String name, int rollNo, double cgpa, String email) {
        this.name = name; this.rollNo = rollNo;
        this.cgpa = cgpa; this.email  = email;
    }
 
    String getName()   { return name;   }
    int    getRollNo() { return rollNo; }
    double getCgpa()   { return cgpa;   }
    String getEmail()  { return email;  }
 
    // PACK: write this student into a ByteBuffer (fixed 92 bytes)
    void pack(ByteBuffer buf) {
        buf.put(padBytes(name,  NAME_LEN));
        buf.put(padBytes(email, EMAIL_LEN));
        buf.putInt(rollNo);
        buf.putDouble(cgpa);
    }
 
    // UNPACK: read a student from a ByteBuffer
    void unpack(ByteBuffer buf) {
        byte[] nb = new byte[NAME_LEN],  eb = new byte[EMAIL_LEN];
        buf.get(nb); buf.get(eb);
        name   = new String(nb).trim();
        email  = new String(eb).trim();
        rollNo = buf.getInt();
        cgpa   = buf.getDouble();
    }
 
    private static byte[] padBytes(String s, int len) {
        byte[] src  = (s == null ? "" : s).getBytes();
        byte[] dest = new byte[len];
        System.arraycopy(src, 0, dest, 0, Math.min(src.length, len));
        return dest;
    }
 
    @Override
    public String toString() {
        return String.format("Student { name=%-16s rollNo=%d  cgpa=%.1f  email=%s }",
                             "'" + name + "'", rollNo, cgpa, email);
    }
}
 
// ── ClassBuffer: manages a flat binary file of Student records ───────────────
class ClassBuffer {
    private final String path;
    ClassBuffer(String path) { this.path = path; }
 
    void writeAll(List<Student> list) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Student.RECORD_LEN * list.size());
        for (Student s : list) s.pack(buf);
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(buf.array());
        }
    }
 
    List<Student> readAll() throws IOException {
        byte[] raw = new FileInputStream(path).readAllBytes();
        ByteBuffer buf = ByteBuffer.wrap(raw);
        List<Student> result = new ArrayList<>();
        while (buf.remaining() >= Student.RECORD_LEN) {
            Student s = new Student(); s.unpack(buf); result.add(s);
        }
        return result;
    }
}
 
// ── Main: tests all 5 IO buffer types ────────────────────────────────────────
public class StudentBufferDemo {
 
    static final List<Student> DATA = Arrays.asList(
        new Student("Alice Johnson",  101, 9.1, "alice@uni.edu"),
        new Student("Bob Smith",      102, 8.4, "bob@uni.edu"),
        new Student("Carol Williams", 103, 7.8, "carol@uni.edu")
    );
 
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("       STUDENT BUFFER DEMO");
        System.out.println("========================================");
 
        test1_ByteBuffer();
        test2_DataStream();
        test3_BufferedReaderWriter();
        test4_ObjectStream();
        test5_ClassBuffer();
 
        System.out.println("\n>> All 5 buffer tests passed!\n");
    }
 
    static void test1_ByteBuffer() {
        System.out.println("\n[TEST 1] java.nio.ByteBuffer - pack() and unpack()");
        System.out.println("----------------------------------------");
        ByteBuffer buf = ByteBuffer.allocate(Student.RECORD_LEN * DATA.size());
        for (Student s : DATA) s.pack(buf);
        buf.flip();
        while (buf.remaining() >= Student.RECORD_LEN) {
            Student s = new Student(); s.unpack(buf);
            System.out.println("  " + s);
        }
    }
 
    static void test2_DataStream() throws IOException {
        System.out.println("\n[TEST 2] DataOutputStream / DataInputStream");
        System.out.println("----------------------------------------");
        String file = "students.bin";
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            for (Student s : DATA) {
                dos.writeUTF(s.getName()); dos.writeInt(s.getRollNo());
                dos.writeDouble(s.getCgpa()); dos.writeUTF(s.getEmail());
            }
        }
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            while (dis.available() > 0) {
                Student s = new Student(dis.readUTF(), dis.readInt(),
                                        dis.readDouble(), dis.readUTF());
                System.out.println("  " + s);
            }
        }
        new File(file).delete();
    }
 
    static void test3_BufferedReaderWriter() throws IOException {
        System.out.println("\n[TEST 3] BufferedWriter / BufferedReader (CSV)");
        System.out.println("----------------------------------------");
        String file = "students.csv";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("name,rollNo,cgpa,email\n");
            for (Student s : DATA)
                bw.write(s.getName()+","+s.getRollNo()+","+s.getCgpa()+","+s.getEmail()+"\n");
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                System.out.println("  " + new Student(p[0], Integer.parseInt(p[1]),
                                                       Double.parseDouble(p[2]), p[3]));
            }
        }
        new File(file).delete();
    }
 
    static void test4_ObjectStream() throws Exception {
        System.out.println("\n[TEST 4] ObjectOutputStream / ObjectInputStream");
        System.out.println("----------------------------------------");
        String file = "students.ser";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new ArrayList<>(DATA));
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            List<Student> list = (List<Student>) ois.readObject();
            list.forEach(s -> System.out.println("  " + s));
        }
        new File(file).delete();
    }
 
    static void test5_ClassBuffer() throws IOException {
        System.out.println("\n[TEST 5] ClassBuffer - Binary File (pack/unpack)");
        System.out.println("----------------------------------------");
        String file = "classfile.cbf";
        ClassBuffer cb = new ClassBuffer(file);
        cb.writeAll(new ArrayList<>(DATA));
        System.out.println("  Written to: " + file);
        cb.readAll().forEach(s -> System.out.println("  " + s));
        new File(file).delete();
    }
}
 
