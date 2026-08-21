import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
 
// ─────────────────────────────────────────────────────────────────────────────
//  Student – serialisable record with pack / unpack support
// ─────────────────────────────────────────────────────────────────────────────
class Student implements Serializable {
 
    private static final long serialVersionUID = 1L;
 
    // Fixed field sizes used by the raw-byte (ByteBuffer) pack / unpack
    private static final int NAME_SIZE  = 40;   // bytes reserved for name
    private static final int EMAIL_SIZE = 40;   // bytes reserved for email
    public  static final int RECORD_SIZE =
            NAME_SIZE + EMAIL_SIZE + Integer.BYTES + Double.BYTES;  // 92 bytes
 
    // ── fields ───────────────────────────────────────────────────────────────
    private String name;
    private int    rollNo;
    private double cgpa;
    private String email;
 
    // ── constructors ─────────────────────────────────────────────────────────
    public Student() {}
 
    public Student(String name, int rollNo, double cgpa, String email) {
        this.name   = name;
        this.rollNo = rollNo;
        this.cgpa   = cgpa;
        this.email  = email;
    }
 
    // ── getters / setters ────────────────────────────────────────────────────
    public String getName()       { return name;   }
    public int    getRollNo()     { return rollNo; }
    public double getCgpa()       { return cgpa;   }
    public String getEmail()      { return email;  }
 
    public void setName(String n)  { this.name   = n; }
    public void setRollNo(int r)   { this.rollNo = r; }
    public void setCgpa(double c)  { this.cgpa   = c; }
    public void setEmail(String e) { this.email  = e; }
 
    // =========================================================================
    //  PACK  –  write this student into a fixed-length ByteBuffer
    //  Layout: [name 40B][email 40B][rollNo 4B][cgpa 8B]  = 92 bytes total
    // =========================================================================
    public void pack(ByteBuffer buf) {
        buf.put(fixedBytes(name,  NAME_SIZE));
        buf.put(fixedBytes(email, EMAIL_SIZE));
        buf.putInt(rollNo);
        buf.putDouble(cgpa);
    }
 
    // =========================================================================
    //  UNPACK  –  read a student from a fixed-length ByteBuffer
    // =========================================================================
    public void unpack(ByteBuffer buf) {
        byte[] nameBytes  = new byte[NAME_SIZE];
        byte[] emailBytes = new byte[EMAIL_SIZE];
        buf.get(nameBytes);
        buf.get(emailBytes);
        this.name   = new String(nameBytes).trim();
        this.email  = new String(emailBytes).trim();
        this.rollNo = buf.getInt();
        this.cgpa   = buf.getDouble();
    }
 
    // ── helper: pad / truncate string to exactly `size` bytes ────────────────
    private static byte[] fixedBytes(String s, int size) {
        byte[] src  = (s == null ? "" : s).getBytes();
        byte[] dest = new byte[size];
        System.arraycopy(src, 0, dest, 0, Math.min(src.length, size));
        return dest;
    }
 
    @Override
    public String toString() {
        return String.format("Student{name='%s', rollNo=%d, cgpa=%.2f, email='%s'}",
                             name, rollNo, cgpa, email);
    }
}
 
// ─────────────────────────────────────────────────────────────────────────────
//  ClassBuffer – manages a flat binary file of fixed-size Student records
// ─────────────────────────────────────────────────────────────────────────────
class ClassBuffer {
 
    private final String filePath;
 
    public ClassBuffer(String filePath) {
        this.filePath = filePath;
    }
 
    /** Write a list of students to the binary file (overwrites). */
    public void writeAll(List<Student> students) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Student.RECORD_SIZE * students.size());
        for (Student s : students) s.pack(buf);
        Files.write(Paths.get(filePath), buf.array());
        System.out.println("[ClassBuffer] Wrote " + students.size()
                           + " record(s) → " + filePath);
    }
 
    /** Read all students from the binary file. */
    public List<Student> readAll() throws IOException {
        byte[]     raw  = Files.readAllBytes(Paths.get(filePath));
        ByteBuffer buf  = ByteBuffer.wrap(raw);
        int        count = raw.length / Student.RECORD_SIZE;
        List<Student> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Student s = new Student();
            s.unpack(buf);
            list.add(s);
        }
        System.out.println("[ClassBuffer] Read " + list.size()
                           + " record(s) ← " + filePath);
        return list;
    }
 
    /** Read a single record by zero-based index using random access. */
    public Student readAt(int index) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek((long) index * Student.RECORD_SIZE);
            byte[] bytes = new byte[Student.RECORD_SIZE];
            raf.readFully(bytes);
            Student s = new Student();
            s.unpack(ByteBuffer.wrap(bytes));
            return s;
        }
    }
}
 
// ─────────────────────────────────────────────────────────────────────────────
//  Main – tests every supported IO-buffer type
// ─────────────────────────────────────────────────────────────────────────────
public class StudentBufferDemo {
 
    // Sample dataset
    private static final List<Student> STUDENTS = Arrays.asList(
        new Student("Alice Johnson",  101, 9.1, "alice@college.edu"),
        new Student("Bob Smith",      102, 8.4, "bob@college.edu"),
        new Student("Carol Williams", 103, 7.8, "carol@college.edu"),
        new Student("David Brown",    104, 9.5, "david@college.edu"),
        new Student("Eve Davis",      105, 8.0, "eve@college.edu")
    );
 
    public static void main(String[] args) throws Exception {
 
        header("STUDENT BUFFER DEMO");
 
        testByteBuffer();
        testDataStream();
        testBufferedStream();
        testObjectStream();
        testClassBuffer();
        testRandomAccess();
        testInMemoryByteArrayStream();
 
        System.out.println("\n✅  All buffer tests completed successfully.");
    }
 
    // =========================================================================
    //  1. ByteBuffer  (java.nio)
    // =========================================================================
    static void testByteBuffer() {
        section("1. java.nio.ByteBuffer  –  pack / unpack");
 
        ByteBuffer buf = ByteBuffer.allocate(Student.RECORD_SIZE * STUDENTS.size());
        for (Student s : STUDENTS) s.pack(buf);
 
        buf.flip();                          // switch to read mode
        List<Student> result = new ArrayList<>();
        while (buf.remaining() >= Student.RECORD_SIZE) {
            Student s = new Student();
            s.unpack(buf);
            result.add(s);
        }
        result.forEach(s -> System.out.println("  " + s));
    }
 
    // =========================================================================
    //  2. DataOutputStream / DataInputStream  (java.io)
    // =========================================================================
    static void testDataStream() throws IOException {
        section("2. DataOutputStream / DataInputStream  –  typed binary I/O");
 
        String path = "students_data.bin";
 
        // ── write ─────────────────────────────────────────────────────────────
        try (DataOutputStream dos =
                new DataOutputStream(new FileOutputStream(path))) {
            for (Student s : STUDENTS) {
                dos.writeUTF(s.getName());
                dos.writeInt(s.getRollNo());
                dos.writeDouble(s.getCgpa());
                dos.writeUTF(s.getEmail());
            }
        }
        System.out.println("  Wrote " + STUDENTS.size() + " records via DataOutputStream.");
 
        // ── read ──────────────────────────────────────────────────────────────
        try (DataInputStream dis =
                new DataInputStream(new FileInputStream(path))) {
            while (dis.available() > 0) {
                Student s = new Student(
                    dis.readUTF(), dis.readInt(), dis.readDouble(), dis.readUTF());
                System.out.println("  " + s);
            }
        }
        new File(path).delete();
    }
 
    // =========================================================================
    //  3. BufferedReader / BufferedWriter  (java.io – text / CSV)
    // =========================================================================
    static void testBufferedStream() throws IOException {
        section("3. BufferedWriter / BufferedReader  –  CSV text I/O");
 
        String path = "students_text.csv";
 
        // ── write ─────────────────────────────────────────────────────────────
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write("name,rollNo,cgpa,email");
            bw.newLine();
            for (Student s : STUDENTS) {
                bw.write(s.getName()   + "," +
                         s.getRollNo() + "," +
                         s.getCgpa()   + "," +
                         s.getEmail());
                bw.newLine();
            }
        }
        System.out.println("  Wrote CSV via BufferedWriter.");
 
        // ── read ──────────────────────────────────────────────────────────────
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            br.readLine();                    // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                Student s  = new Student(p[0], Integer.parseInt(p[1]),
                                         Double.parseDouble(p[2]), p[3]);
                System.out.println("  " + s);
            }
        }
        new File(path).delete();
    }
 
    // =========================================================================
    //  4. ObjectOutputStream / ObjectInputStream  (java.io – serialisation)
    // =========================================================================
    static void testObjectStream() throws IOException, ClassNotFoundException {
        section("4. ObjectOutputStream / ObjectInputStream  –  Java serialisation");
 
        String path = "students_objects.ser";
 
        // ── write ─────────────────────────────────────────────────────────────
        try (ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(new ArrayList<>(STUDENTS));
        }
        System.out.println("  Serialised " + STUDENTS.size() + " students.");
 
        // ── read ──────────────────────────────────────────────────────────────
        try (ObjectInputStream ois =
                new ObjectInputStream(new FileInputStream(path))) {
            @SuppressWarnings("unchecked")
            List<Student> list = (List<Student>) ois.readObject();
            list.forEach(s -> System.out.println("  " + s));
        }
        new File(path).delete();
    }
 
    // =========================================================================
    //  5. ClassBuffer (fixed-size binary file)
    // =========================================================================
    static void testClassBuffer() throws IOException {
        section("5. ClassBuffer  –  fixed-size binary file (pack/unpack)");
 
        String      path = "classfile.cbf";
        ClassBuffer cb   = new ClassBuffer(path);
 
        cb.writeAll(new ArrayList<>(STUDENTS));
        List<Student> read = cb.readAll();
        read.forEach(s -> System.out.println("  " + s));
        new File(path).delete();
    }
 
    // =========================================================================
    //  6. RandomAccessFile  –  direct-seek record read
    // =========================================================================
    static void testRandomAccess() throws IOException {
        section("6. RandomAccessFile  –  seek-and-read a single record");
 
        String      path = "classfile_ra.cbf";
        ClassBuffer cb   = new ClassBuffer(path);
        cb.writeAll(new ArrayList<>(STUDENTS));
 
        int targetIndex = 2;
        Student s = cb.readAt(targetIndex);
        System.out.println("  Record at index " + targetIndex + ": " + s);
        new File(path).delete();
    }
 
    // =========================================================================
    //  7. ByteArrayOutputStream / ByteArrayInputStream  –  in-memory buffer
    // =========================================================================
    static void testInMemoryByteArrayStream() throws IOException {
        section("7. ByteArrayOutputStream / ByteArrayInputStream  –  in-memory");
 
        // ── pack into in-memory byte array ────────────────────────────────────
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            for (Student s : STUDENTS) {
                dos.writeUTF(s.getName());
                dos.writeInt(s.getRollNo());
                dos.writeDouble(s.getCgpa());
                dos.writeUTF(s.getEmail());
            }
        }
        System.out.println("  Packed into in-memory buffer: "
                           + baos.size() + " bytes.");
 
        // ── unpack from in-memory byte array ──────────────────────────────────
        try (DataInputStream dis =
                new DataInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            while (dis.available() > 0) {
                Student s = new Student(
                    dis.readUTF(), dis.readInt(), dis.readDouble(), dis.readUTF());
                System.out.println("  " + s);
            }
        }
    }
 
    // ── formatting helpers ────────────────────────────────────────────────────
    static void header(String t) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("  " + t);
        System.out.println("═".repeat(60));
    }
    static void section(String t) {
        System.out.println("\n┌─ " + t);
        System.out.println("└" + "─".repeat(58));
    }
}
 
