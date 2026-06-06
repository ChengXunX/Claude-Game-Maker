#!/usr/bin/env node

/**
 * ChengXun Game Maker MCP 工具服务器
 * 提供后端 API 的工具，让 Claude 可以直接调用后端 API
 */

const http = require('http');

// 后端 API 基础 URL（使用内部 API，不需要认证）
const API_BASE_URL = 'http://127.0.0.1:19922/api/internal';

// 用户 Token（从环境变量获取）
const USER_TOKEN = process.env.USER_TOKEN || '';

// MCP 工具定义
const TOOLS = [
  {
    name: 'list_workflow_templates',
    description: '获取所有工作流模板列表',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'create_workflow_template',
    description: '创建工作流模板',
    inputSchema: {
      type: 'object',
      properties: {
        id: { type: 'string', description: '模板ID（英文标识）' },
        name: { type: 'string', description: '模板名称' },
        description: { type: 'string', description: '模板描述' }
      },
      required: ['id', 'name']
    }
  },
  {
    name: 'delete_workflow_template',
    description: '删除工作流模板',
    inputSchema: {
      type: 'object',
      properties: {
        templateId: { type: 'string', description: '要删除的模板ID' }
      },
      required: ['templateId']
    }
  },
  {
    name: 'list_agents',
    description: '获取所有 Agent 列表和状态',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'list_projects',
    description: '获取所有游戏项目列表',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'create_project',
    description: '创建新的游戏项目',
    inputSchema: {
      type: 'object',
      properties: {
        name: { type: 'string', description: '项目名称' },
        description: { type: 'string', description: '项目描述' }
      },
      required: ['name']
    }
  },
  {
    name: 'list_game_templates',
    description: '获取所有游戏模板列表',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'create_game_template',
    description: '创建游戏模板',
    inputSchema: {
      type: 'object',
      properties: {
        name: { type: 'string', description: '模板名称' },
        description: { type: 'string', description: '模板描述' },
        gameType: { type: 'string', description: '游戏类型' }
      },
      required: ['name']
    }
  },
  {
    name: 'list_skills',
    description: '获取所有技能列表',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'create_skill',
    description: '创建技能',
    inputSchema: {
      type: 'object',
      properties: {
        name: { type: 'string', description: '技能名称' },
        description: { type: 'string', description: '技能描述' },
        prompt: { type: 'string', description: '技能提示词' },
        category: { type: 'string', description: '技能分类' }
      },
      required: ['name', 'description', 'prompt']
    }
  },
  {
    name: 'list_workflow_instances',
    description: '获取所有运行中的工作流实例',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'start_workflow',
    description: '启动工作流实例',
    inputSchema: {
      type: 'object',
      properties: {
        templateId: { type: 'string', description: '工作流模板ID' },
        projectId: { type: 'string', description: '项目ID' }
      },
      required: ['templateId', 'projectId']
    }
  },
  {
    name: 'get_agent_health',
    description: '获取 Agent 健康状态',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'list_alerts',
    description: '获取系统告警列表',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'get_system_info',
    description: '获取系统运行环境信息',
    inputSchema: {
      type: 'object',
      properties: {},
      required: []
    }
  },
  {
    name: 'call_api',
    description: '调用系统任意 API 端点',
    inputSchema: {
      type: 'object',
      properties: {
        method: { type: 'string', description: 'HTTP 方法（GET/POST/PUT/DELETE）' },
        path: { type: 'string', description: 'API 路径' },
        body: { type: 'string', description: '请求体（JSON 格式）' }
      },
      required: ['method', 'path']
    }
  }
];

// API 路径映射
const API_PATHS = {
  'list_workflow_templates': { method: 'GET', path: '/workflow-templates' },
  'create_workflow_template': { method: 'POST', path: '/workflow-templates' },
  'delete_workflow_template': { method: 'DELETE', path: '/workflow-templates/{templateId}' },
  'list_agents': { method: 'GET', path: '/agents' },
  'list_projects': { method: 'GET', path: '/projects' },
  'create_project': { method: 'POST', path: '/projects' },
  'list_game_templates': { method: 'GET', path: '/game-templates' },
  'create_game_template': { method: 'POST', path: '/game-templates' },
  'list_skills': { method: 'GET', path: '/skills' },
  'create_skill': { method: 'POST', path: '/skills' },
  'list_workflow_instances': { method: 'GET', path: '/workflows/instances' },
  'start_workflow': { method: 'POST', path: '/workflows/start' },
  'get_agent_health': { method: 'GET', path: '/agents/health' },
  'list_alerts': { method: 'GET', path: '/alerts' },
  'get_system_info': { method: 'GET', path: '/system/info' }
};

/**
 * 调用后端 API
 */
async function callApi(method, path, body) {
  return new Promise((resolve, reject) => {
    const fullPath = API_BASE_URL + path;

    const options = {
      hostname: '127.0.0.1',
      port: 19922,
      path: fullPath,
      method: method,
      headers: {
        'Content-Type': 'application/json'
      }
    };

    // 添加用户 Token
    if (USER_TOKEN) {
      options.headers['Authorization'] = `Bearer ${USER_TOKEN}`;
    }

    const req = http.request(options, (res) => {
      let data = '';

      res.on('data', (chunk) => {
        data += chunk;
      });

      res.on('end', () => {
        try {
          const jsonData = JSON.parse(data);
          resolve({
            statusCode: res.statusCode,
            data: jsonData
          });
        } catch (e) {
          resolve({
            statusCode: res.statusCode,
            data: data
          });
        }
      });
    });

    req.on('error', (error) => {
      reject(error);
    });

    if (body) {
      req.write(JSON.stringify(body));
    }

    req.end();
  });
}

/**
 * 处理工具调用
 */
async function handleToolCall(toolName, args) {
  // 处理通用 API 调用
  if (toolName === 'call_api') {
    const { method, path, body } = args;
    try {
      const result = await callApi(method, path, body ? JSON.parse(body) : null);
      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(result.data, null, 2)
          }
        ]
      };
    } catch (error) {
      return {
        content: [
          {
            type: 'text',
            text: `API 调用失败: ${error.message}`
          }
        ],
        isError: true
      };
    }
  }

  // 处理预定义的工具
  const apiConfig = API_PATHS[toolName];
  if (!apiConfig) {
    return {
      content: [
        {
          type: 'text',
          text: `未知工具: ${toolName}`
        }
      ],
      isError: true
    };
  }

  let { method, path } = apiConfig;
  let body = null;

  // 处理路径参数
  if (args.templateId) {
    path = path.replace('{templateId}', args.templateId);
  }

  // 处理请求体
  if (method === 'POST' || method === 'PUT') {
    body = args;
  }

  try {
    const result = await callApi(method, path, body);
    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result.data, null, 2)
        }
      ]
    };
  } catch (error) {
    return {
      content: [
        {
          type: 'text',
          text: `API 调用失败: ${error.message}`
        }
      ],
      isError: true
    };
  }
}

/**
 * 处理 MCP 请求
 */
async function handleRequest(request) {
  const { method, params, id } = request;

  switch (method) {
    case 'initialize':
      return {
        jsonrpc: '2.0',
        id,
        result: {
          protocolVersion: '2024-11-05',
          capabilities: {
            tools: {}
          },
          serverInfo: {
            name: 'chengxun-game-maker',
            version: '1.0.0'
          }
        }
      };

    case 'tools/list':
      return {
        jsonrpc: '2.0',
        id,
        result: {
          tools: TOOLS
        }
      };

    case 'tools/call':
      const { name, arguments: args } = params;
      const result = await handleToolCall(name, args || {});
      return {
        jsonrpc: '2.0',
        id,
        result
      };

    default:
      return {
        jsonrpc: '2.0',
        id,
        error: {
          code: -32601,
          message: `Method not found: ${method}`
        }
      };
  }
}

/**
 * 主函数
 */
async function main() {
  // 读取 stdin
  let buffer = '';
  let pendingRequests = 0;

  process.stdin.setEncoding('utf8');

  process.stdin.on('data', async (chunk) => {
    buffer += chunk;

    // 尝试解析 JSON-RPC 请求
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      if (line.trim()) {
        pendingRequests++;
        try {
          const request = JSON.parse(line);
          const response = await handleRequest(request);
          process.stdout.write(JSON.stringify(response) + '\n');
        } catch (error) {
          process.stderr.write(`Error parsing request: ${error.message}\n`);
        }
        pendingRequests--;
      }
    }
  });

  process.stdin.on('end', () => {
    // 等待所有待处理请求完成
    const checkPending = () => {
      if (pendingRequests === 0) {
        process.exit(0);
      } else {
        setTimeout(checkPending, 100);
      }
    };
    checkPending();
  });
}

main();
