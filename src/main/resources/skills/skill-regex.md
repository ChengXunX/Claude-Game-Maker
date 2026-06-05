---
name: 正则表达式
description: 正则表达式编写和应用
trigger: 正则表达式,regex,正则,模式匹配,文本处理
examples: 正则匹配|文本提取|数据验证|字符串处理
---

# 正则表达式技能

## 基础语法

### 字符匹配
| 语法 | 说明 | 示例 |
|------|------|------|
| `.` | 任意字符 | `a.c` → abc, aXc |
| `\d` | 数字 | `\d+` → 123 |
| `\w` | 字母数字 | `\w+` → hello |
| `\s` | 空白字符 | `\s+` → 空格 |
| `[abc]` | 字符集 | `[aeiou]` → 元音 |
| `[^abc]` | 排除 | `[^0-9]` → 非数字 |

### 量词
| 语法 | 说明 | 示例 |
|------|------|------|
| `*` | 0次或多次 | `ab*c` → ac, abc |
| `+` | 1次或多次 | `ab+c` → abc, abbc |
| `?` | 0次或1次 | `colou?r` → color, colour |
| `{n}` | 恰好n次 | `\d{3}` → 123 |
| `{n,m}` | n到m次 | `\d{2,4}` → 12, 1234 |

### 位置
| 语法 | 说明 |
|------|------|
| `^` | 开头 |
| `$` | 结尾 |
| `\b` | 单词边界 |

## 常用模式

### 邮箱验证
```
^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$
```

### 手机号验证
```
^1[3-9]\d{9}$
```

### 身份证号
```
^\d{17}[\dXx]$
```

### URL匹配
```
https?://[^\s]+
```

### IP地址
```
^(\d{1,3}\.){3}\d{1,3}$
```

## Java使用

```java
// 匹配
Pattern pattern = Pattern.compile("\\d+");
Matcher matcher = pattern.matcher("abc123def456");
while (matcher.find()) {
    System.out.println(matcher.group()); // 123, 456
}

// 替换
String result = "hello123".replaceAll("\\d", "*"); // hello***

// 分割
String[] parts = "a,b,,c".split(","); // ["a", "b", "", "c"]
```

## JavaScript使用

```javascript
// 匹配
const regex = /\d+/g;
const str = "abc123def456";
str.match(regex); // ["123", "456"]

// 替换
"hello123".replace(/\d/g, "*"); // hello***

// 测试
/^\w+@\w+\.\w+$/.test("test@example.com"); // true
```
