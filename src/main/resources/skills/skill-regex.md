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
| `*` | 0次或多次 | `a*` → "", a, aa |
| `+` | 1次或多次 | `a+` → a, aa |
| `?` | 0次或1次 | `a?` → "", a |
| `{n}` | 恰好n次 | `a{3}` → aaa |
| `{n,}` | 至少n次 | `a{2,}` → aa, aaa |
| `{n,m}` | n到m次 | `a{2,4}` → aa, aaa, aaaa |

### 位置
| 语法 | 说明 | 示例 |
|------|------|------|
| `^` | 开头 | `^hello` |
| `$` | 结尾 | `world$` |
| `\b` | 单词边界 | `\bword\b` |

### 分组
| 语法 | 说明 | 示例 |
|------|------|------|
| `()` | 捕获组 | `(abc)+` |
| `(?:)` | 非捕获组 | `(?:abc)+` |
| `(?=)` | 前瞻 | `a(?=b)` |
| `(?!)` | 负前瞻 | `a(?!b)` |

## 常用正则

### 1. 邮箱验证
```regex
^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$
```

### 2. 手机号验证
```regex
^1[3-9]\d{9}$
```

### 3. 身份证验证
```regex
^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$
```

### 4. URL验证
```regex
https?:\/\/(www\.)?[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)
```

### 5. IP地址验证
```regex
^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$
```

## 游戏正则

### 1. 玩家名称验证
```javascript
// 2-12位中文、英文、数字
const nameRegex = /^[一-龥a-zA-Z0-9]{2,12}$/;

// 验证
function validatePlayerName(name) {
  return nameRegex.test(name);
}
```

### 2. 聊天内容过滤
```javascript
// 过滤敏感词
const sensitiveWords = ['作弊', '外挂', 'hack'];
const filterRegex = new RegExp(sensitiveWords.join('|'), 'gi');

function filterChat(text) {
  return text.replace(filterRegex, '***');
}
```

### 3. 游戏命令解析
```javascript
// 解析命令 /command arg1 arg2
const commandRegex = /^\/(\w+)\s*(.*)?$/;

function parseCommand(input) {
  const match = input.match(commandRegex);
  if (match) {
    return {
      command: match[1],
      args: match[2] ? match[2].split(/\s+/) : []
    };
  }
  return null;
}
```

### 4. 数值提取
```javascript
// 提取伤害数值
const damageRegex = /造成(\d+)点伤害/;

function extractDamage(text) {
  const match = text.match(damageRegex);
  return match ? parseInt(match[1]) : 0;
}
```

### 5. 时间格式解析
```javascript
// 解析时间 00:00:00
const timeRegex = /^(\d{2}):(\d{2}):(\d{2})$/;

function parseTime(timeStr) {
  const match = timeStr.match(timeRegex);
  if (match) {
    return {
      hours: parseInt(match[1]),
      minutes: parseInt(match[2]),
      seconds: parseInt(match[3])
    };
  }
  return null;
}
```

## JavaScript正则

### 1. 基本用法
```javascript
// 创建正则
const regex1 = /pattern/;
const regex2 = new RegExp('pattern');

// 匹配
const str = 'hello world';
const result = str.match(/world/);

// 测试
const isValid = regex.test(str);

// 替换
const newStr = str.replace(/world/, 'game');
```

### 2. 全局匹配
```javascript
const str = 'a1b2c3';
const numbers = str.match(/\d/g); // ['1', '2', '3']
```

### 3. 分组捕获
```javascript
const str = '2026-06-09';
const match = str.match(/(\d{4})-(\d{2})-(\d{2})/);
if (match) {
  const year = match[1];  // '2026'
  const month = match[2]; // '06'
  const day = match[3];   // '09'
}
```

## Java正则

### 1. 基本用法
```java
// 编译正则
Pattern pattern = Pattern.compile("\\d+");
Matcher matcher = pattern.matcher("abc123def456");

// 查找
while (matcher.find()) {
  System.out.println(matcher.group());
}

// 匹配
boolean matches = pattern.matcher("123").matches();
```

### 2. 分组捕获
```java
Pattern pattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
Matcher matcher = pattern.matcher("2026-06-09");
if (matcher.matches()) {
  String year = matcher.group(1);
  String month = matcher.group(2);
  String day = matcher.group(3);
}
```

## 常见错误

1. **转义错误**：要正确转义特殊字符
2. **贪婪匹配**：要使用非贪婪匹配
3. **性能问题**：要避免回溯
4. **边界问题**：要注意边界匹配
5. **编码问题**：要注意字符编码
