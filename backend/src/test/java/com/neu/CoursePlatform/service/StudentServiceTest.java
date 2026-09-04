package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.mapper.StudentMapper;
import com.neu.CoursePlatform.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.neu.CoursePlatform.service.TeacherServiceTest.setBaseMapper;
import static org.junit.jupiter.api.Assertions.*;

class StudentServiceTest {

    private StudentServiceImpl service;
    private Map<String, Student> store;

    @BeforeEach
    void setUp() throws Exception {
        store = new LinkedHashMap<>();
        Student s = student("2024001", "zhangsan", "123456", "张三");
        store.put("2024001", s);

        StudentMapper proxy = (StudentMapper) Proxy.newProxyInstance(
                StudentMapper.class.getClassLoader(),
                new Class<?>[]{StudentMapper.class},
                (p, method, args) -> studentInvoke(store, method.getName(), args)
        );

        service = new StudentServiceImpl();
        setBaseMapper(service, proxy);
    }

    // ============ 登录 ============

    @Test
    void loginSuccess() {
        Student r = service.login("zhangsan", "123456");
        assertNotNull(r);
        assertEquals("张三", r.getName());
        assertTrue(r.getPassword().startsWith("$2"));
        assertNotEquals("123456", r.getPassword());
    }

    @Test
    void loginFailWrongPassword() {
        assertNull(service.login("zhangsan", "wrong"));
    }

    @Test
    void loginFailNonexistent() {
        assertNull(service.login("nobody", "any"));
    }

    @Test
    void loginFailEmptyInput() {
        assertNull(service.login("", ""));
        assertNull(service.login(null, "pass"));
    }

    // ============ 注册 ============

    @Test
    void registerSuccess() {
        Student s = student("2024002", "lisi", "pass", "李四");
        assertTrue(service.register(s));
        assertTrue(s.getPassword().startsWith("$2"));
        assertNotNull(service.login("lisi", "pass"));
    }

    @Test
    void registerFailDuplicate() {
        Student dup = student("9999", "zhangsan", "pass", "重名");
        assertFalse(service.register(dup));
    }

    // ============ 搜索 ============

    @Test
    void searchFindsByName() {
        assertEquals(1, service.searchByKeyword("张").size());
    }

    @Test
    void searchEmptyReturnsAll() {
        store.put("2024002", student("2024002", "u2", "pw", "T2"));
        assertEquals(2, service.searchByKeyword("").size());
    }

    // ============ 按班级查询 ============

    @Test
    void listByClassId() {
        Student s2 = student("2024003", "wangwu", "pw", "王五");
        s2.setClassName("计科202班");
        store.put("2024003", s2);
        assertEquals(2, service.listByClassId("计科202班").size());
    }

    @Test
    void listByClassIdNullReturnsAll() {
        assertEquals(1, service.listByClassId(null).size());
        assertEquals(1, service.listByClassId("").size());
    }

    // ============ 导入 ============

    @Test
    void importFromCsvSuccess() throws Exception {
        String csv = "学号,姓名,学院,班级,用户名,密码,手机\n"
                + "2024005,赵六,计算机,计科201,zhaoliu,pw123,139";
        int count = service.importFromExcel(csvFile(csv));
        assertEquals(1, count);
        assertNotNull(service.login("zhaoliu", "pw123"));
    }

    @Test
    void importSkipsDuplicateThrowsWithErrorDetail() {
        String csv = "学号,姓名,学院,班级,用户名,密码\n2024001,重复,计算机,计科,dup,pw";
        IOException ex = assertThrows(IOException.class,
                () -> service.importFromExcel(csvFile(csv)));
        assertTrue(ex.getMessage().contains("已存在"));
    }

    @Test
    void importEmptyFileReturnsZero() throws Exception {
        // 空文件跳过 header 后无数据，返回 0
        String csv = "学号,姓名,学院,班级,用户名,密码\n";
        assertEquals(0, service.importFromExcel(csvFile(csv)));
    }

    // ============ 导出 ============

    @Test
    void exportIncludesHeaderAndData() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportToExcel(out);
        String result = out.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("姓名,学院,班级,用户名"));
        assertTrue(result.contains("张三"));
    }

    // ============ helpers ============

    private Student student(String no, String username, String password, String name) {
        Student s = new Student();
        s.setStudentNo(no);
        s.setUsername(username);
        s.setPassword(password);
        s.setName(name);
        s.setCollege("计算机学院");
        s.setClassName("计科202班");
        return s;
    }

    private org.springframework.web.multipart.MultipartFile csvFile(String content) {
        return new org.springframework.web.multipart.MultipartFile() {
            @Override public String getName() { return "test.csv"; }
            @Override public String getOriginalFilename() { return "test.csv"; }
            @Override public String getContentType() { return "text/csv"; }
            @Override public boolean isEmpty() { return content.isEmpty(); }
            @Override public long getSize() { return content.length(); }
            @Override public byte[] getBytes() { return content.getBytes(StandardCharsets.UTF_8); }
            @Override public InputStream getInputStream() { return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)); }
            @Override public void transferTo(File dest) { throw new UnsupportedOperationException(); }
        };
    }

    static Object studentInvoke(Map<String, Student> store, String methodName, Object[] args) {
        switch (methodName) {
            case "selectByUsername": {
                String u = (String) args[0];
                return store.values().stream()
                        .filter(s -> u != null && u.equals(s.getUsername())).findFirst().orElse(null);
            }
            case "selectByKeyword": {
                String kw = (String) args[0];
                if (kw == null || kw.isEmpty()) return new ArrayList<>(store.values());
                return store.values().stream()
                        .filter(s -> (s.getName() != null && s.getName().contains(kw))
                                || (s.getCollege() != null && s.getCollege().contains(kw))).toList();
            }
            case "selectByClassId": {
                String cid = (String) args[0];
                return store.values().stream()
                        .filter(s -> cid != null && cid.equals(s.getClassName())).toList();
            }
            case "insert": {
                if (args != null && args.length == 1 && args[0] instanceof Student s) {
                    if (s.getStudentNo() == null) s.setStudentNo(String.valueOf(store.size() + 1));
                    store.put(s.getStudentNo(), s);
                    return 1;
                }
                return 0;
            }
            case "selectById": return store.get(String.valueOf(args[0]));
            case "selectList": return new ArrayList<>(store.values());
            case "updateById": { Student s = (Student) args[0]; store.put(s.getStudentNo(), s); return 1; }
            case "deleteById": return store.remove(String.valueOf(args[0])) != null ? 1 : 0;
            case "selectCount": return (long) store.size();
            default: return null;
        }
    }
}
