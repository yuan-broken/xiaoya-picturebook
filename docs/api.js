/**
 * 小芽绘本 AI 伴读成长空间 - 公共 API 封装
 *
 * 维护人：Agent-5（公共文件，统一维护）
 * 其他 Agent 需加方法时在任务看板写「公共文件请求」段落
 *
 * 功能：
 *   - 统一 Token 管理
 *   - 统一错误处理
 *   - 封装所有用户端与管理端 API
 */

const API = (function () {
  // ============================================================
  // 配置
  // ============================================================
  const BASE_URL = 'https://circumstances-standards-cookbook-washer.trycloudflare.com'; // 生产环境 tunnel (http2)
  const TOKEN_KEY = 'picturebook_token';
  const USER_KEY = 'picturebook_user';

  // ============================================================
  // Token 管理
  // ============================================================
  function getToken() {
    return localStorage.getItem(TOKEN_KEY) || '';
  }

  function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
  }

  function getUser() {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || '{}');
    } catch (e) {
      return {};
    }
  }

  function setUser(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  function clearAuth() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  function isLoggedIn() {
    return !!getToken();
  }

  function isAdmin() {
    return getUser().role === 'admin';
  }

  // ============================================================
  // 请求封装
  // ============================================================
  async function request(method, url, data, options) {
    options = options || {};
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }
    const config = {
      method: method,
      headers: headers,
      ...options,
    };
    if (data && method !== 'GET' && method !== 'HEAD') {
      config.body = JSON.stringify(data);
    }
    try {
      const response = await fetch(BASE_URL + url, config);
      // 处理非 JSON 响应
      const contentType = response.headers.get('content-type') || '';
      let result;
      if (contentType.includes('application/json')) {
        result = await response.json();
      } else {
        result = { code: response.ok ? 200 : 500, msg: response.statusText, data: await response.text() };
      }
      // 统一错误处理
      if (response.status === 401 || response.status === 403) {
        // 测试 token 不清除认证，保持测试会话（仅未部署后端时使用）
        if (!token.startsWith('test-token-')) {
          clearAuth();
          // 跳转登录（避免在登录页跳转循环）
          if (!window.location.pathname.includes('login')) {
            console.warn('认证失效，请重新登录');
          }
        }
      }
      return result;
    } catch (e) {
      console.error('API请求失败:', url, e);
      return { code: 500, msg: '网络异常，请稍后重试', data: null };
    }
  }

  function get(url) {
    return request('GET', url);
  }

  function post(url, data) {
    return request('POST', url, data);
  }

  function put(url, data) {
    return request('PUT', url, data);
  }

  function del(url) {
    return request('DELETE', url);
  }

  /**
   * 文件上传（使用 FormData）
   */
  async function upload(file, type) {
    const formData = new FormData();
    formData.append('file', file);
    if (type) formData.append('type', type);
    const headers = {};
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    try {
      const response = await fetch(BASE_URL + '/api/file/upload', {
        method: 'POST',
        headers: headers,
        body: formData,
      });
      const result = await response.json();
      return result;
    } catch (e) {
      console.error('文件上传失败:', e);
      return { code: 500, msg: '文件上传失败', data: null };
    }
  }

  // ============================================================
  // 认证 API（Agent-4 维护后端）
  // ============================================================
  const auth = {
    /** 家长登录 */
    login: (username, password) => post('/api/auth/login', { username, password }),
    /** 孩子登录 */
    childLogin: (username, password) => post('/api/auth/child/login', { username, password }),
    /** 获取当前用户信息 */
    me: () => get('/api/auth/me'),
    /** 退出：调后端 logout 接口，然后清理本地 token */
    logout: () => post('/api/auth/logout').catch(() => {}).then(() => { clearAuth(); return { code: 200, msg: '已退出' }; }),
    /** 是否已登录为孩子角色 */
    isChild: () => getUser().role === 'child',
    /** 是否已登录为家长角色 */
    isParent: () => {
      const r = getUser().role;
      return r === 'parent' || (!r && isLoggedIn()); // 兼容旧 token 无 role
    },
  };

  // ============================================================
  // 首页 API（Agent-4 维护后端）
  // ============================================================
  const home = {
    /** 获取首页数据：成长数据 + 最近伴读任务 + 今日推荐 */
    getData: () => get('/api/user/home'),
    /** 获取家庭信息 */
    getFamily: () => get('/api/user/family'),
    /** 获取孩子档案 */
    getChildren: () => get('/api/user/children'),
    /** 获取用户设置 */
    getSettings: () => get('/api/user/settings'),
    /** 保存用户设置 */
    saveSettings: (settings) => put('/api/user/settings', settings),
  };

  // ============================================================
  // 绘本库 API（Agent-1 维护后端）
  // ============================================================
  const book = {
    /**
     * 绘本列表
     * @param {Object} params { keyword, ageGroup, theme, pageNum, pageSize }
     */
    list: (params) => get('/api/book/list' + buildQuery(params)),
    /** 绘本详情（含页面和互动点） */
    detail: (id) => get('/api/book/detail/' + id),
    /** 绘本页面列表 */
    pages: (id) => get('/api/book/' + id + '/pages'),
    /** 绘本互动点 */
    interactions: (id) => get('/api/book/' + id + '/interactions'),
  };

  // ============================================================
  // AI 伴读 API（Agent-2 维护后端）
  // ============================================================
  const reading = {
    /** 创建讲读任务 */
    createTask: (bookId, childId, readingStyle) => post('/api/reading/task/create', { bookId, childId, readingStyle }),
    /** 获取讲读任务状态 */
    getTask: (id) => get('/api/reading/task/' + id),
    /** 下一页 */
    nextPage: (id) => post('/api/reading/task/' + id + '/next-page'),
    /** 上一页 */
    prevPage: (id) => post('/api/reading/task/' + id + '/prev-page'),
    /** 提交互动回应 */
    submitInteraction: (id, data) => post('/api/reading/task/' + id + '/interaction', data),
    /** 暂停 */
    pause: (id) => post('/api/reading/task/' + id + '/pause'),
    /** 继续 */
    resume: (id) => post('/api/reading/task/' + id + '/resume'),
    /** 获取互动点 */
    getInteractions: (id) => get('/api/reading/task/' + id + '/interactions'),
  };

  // ============================================================
  // 故事创作 API（Agent-3 维护后端）
  // ============================================================
  const story = {
    /** 角色列表 */
    roles: () => get('/api/role/list'),
    /** 主题列表 */
    themes: () => get('/api/theme/list'),
    /** 生成故事（调用后端 AI LLM） */
    generate: (roleId, roleName, themeId, storyInput) =>
      post('/api/story/create', { roleId, roleName, themeId, storyInput }),
    /** 获取故事详情 */
    detail: (id) => get('/api/story/' + id),
    /** 我的故事列表 */
    my: () => get('/api/story/my'),
  };

  // ============================================================
  // 角色对话 API（Agent-3 维护后端）
  // ============================================================
  const chat = {
    /** 创建或恢复对话会话（后端 POST /api/chat/session/{roleId}） */
    createSession: (roleId) => post('/api/chat/session/' + roleId, {}),
    /** 发送消息（后端 POST /api/chat/send，body 为 SendMessageRequest） */
    sendMessage: (sessionId, content, roleId, opts) =>
      post('/api/chat/send', Object.assign({ sessionId, content, roleId, messageType: 'text' }, opts || {})),
    /** 获取会话消息列表 */
    messages: (sessionId) => get('/api/chat/' + sessionId + '/messages'),
  };

  // ============================================================
  // 文件上传 API（Agent-5 维护）
  // ============================================================
  const file = {
    /** 上传文件 */
    upload: (file, type) => upload(file, type),
  };

  // ============================================================
  // 管理端 - 统计 API（Agent-5 维护）
  // ============================================================
  const adminStats = {
    /** 数据概览 */
    overview: () => get('/api/admin/stats/overview'),
    /** 完整统计 */
    full: () => get('/api/admin/stats/full'),
    /** 热门绘本 */
    hotBooks: (limit) => get('/api/admin/stats/hot-books' + buildQuery({ limit })),
    /** 热门角色 */
    hotRoles: (limit) => get('/api/admin/stats/hot-roles' + buildQuery({ limit })),
  };

  // ============================================================
  // 管理端 - 系统配置 API（Agent-5 维护）
  // ============================================================
  const adminConfig = {
    /** 配置列表 */
    list: () => get('/api/admin/config/list'),
    /** 获取单个配置 */
    get: (key) => get('/api/admin/config/' + key),
    /** 按前缀批量获取 */
    map: (prefix) => get('/api/admin/config/map' + buildQuery({ prefix })),
    /** 保存配置 */
    save: (key, value) => put('/api/admin/config', { configKey: key, configValue: value }),
    /** 防沉迷配置 */
    antiAddiction: () => get('/api/admin/config/anti-addiction'),
    /** 产品配置 */
    product: () => get('/api/admin/config/product'),
  };

  // ============================================================
  // 管理端 - AI 模型配置 API（Agent-5 维护）
  // ============================================================
  const adminAiModel = {
    /** 分页查询 */
    list: (pageNum, pageSize, capabilityType, provider) =>
      get('/api/admin/ai-model/list' + buildQuery({ pageNum, pageSize, capabilityType, provider })),
    /** 启用的配置 */
    enabled: () => get('/api/admin/ai-model/enabled'),
    /** 新增 */
    add: (model) => post('/api/admin/ai-model', model),
    /** 编辑 */
    update: (model) => put('/api/admin/ai-model', model),
    /** 删除 */
    remove: (id) => del('/api/admin/ai-model/' + id),
    /** 切换启用 */
    toggle: (id) => put('/api/admin/ai-model/' + id + '/toggle'),
  };

  // ============================================================
  // 激励与习惯 API（二期 M36）
  // ============================================================
  const habit = {
    /** 阅读打卡 */
    checkin: (childId, readingMinutes, bookId, taskId) =>
      post('/api/habit/checkin', { childId, readingMinutes, bookId, taskId }),
    /** 打卡记录 */
    checkinList: (childId, days) =>
      get('/api/habit/checkin/list' + buildQuery({ childId, days: days || 30 })),
    /** 打卡统计 */
    checkinStats: (childId) => get('/api/habit/checkin/stats' + buildQuery({ childId })),
    /** 所有勋章 */
    badges: () => get('/api/habit/badge/list'),
    /** 我的勋章 */
    myBadges: (childId) => get('/api/habit/badge/my' + buildQuery({ childId })),
    /** 手动颁发勋章 */
    awardBadge: (childId, badgeCode, remark) =>
      post('/api/habit/badge/award', { childId, badgeCode, remark }),
    /** 检查并颁发勋章 */
    checkBadges: (childId) => post('/api/habit/badge/check', { childId }),
    /** 添加收藏 */
    addFavorite: (fav) => post('/api/habit/favorite', fav),
    /** 取消收藏 */
    removeFavorite: (id) => del('/api/habit/favorite/' + id),
    /** 切换收藏 */
    toggleFavorite: (fav) => post('/api/habit/favorite/toggle', fav),
    /** 收藏列表 */
    favorites: (childId, targetType, groupName) =>
      get('/api/habit/favorite/list' + buildQuery({ childId, targetType, groupName })),
  };

  // ============================================================
  // 个性化推荐 API（二期 M35）
  // ============================================================
  const recommend = {
    /** 孩子画像 */
    profile: (childId) => get('/api/recommend/profile' + buildQuery({ childId })),
    /** 推荐绘本 */
    books: (childId, limit) => get('/api/recommend/books' + buildQuery({ childId, limit })),
    /** 今日推荐 */
    today: (childId, limit) => get('/api/recommend/today' + buildQuery({ childId, limit })),
    /** 难度自适应 */
    depth: (childId, bookId) => get('/api/recommend/depth' + buildQuery({ childId, bookId })),
  };

  // ============================================================
  // AI 能力 API（二期 M30/M31 测试用）
  // ============================================================
  const ai = {
    /** TTS 合成 */
    tts: (text, voiceId, style) => post('/api/ai/tts/synthesize', { text, voiceId, style }),
    /** 按角色合成 */
    ttsByRole: (roleId, text, style) => post('/api/ai/tts/synthesize-by-role', { roleId, text, style }),
    /** 流式合成 */
    ttsStream: (text, voiceId, style) => post('/api/ai/tts/stream', { text, voiceId, style }),
    /** 音色列表 */
    voices: () => get('/api/ai/tts/voices'),
    /** ASR 识别 */
    asr: (audioUrl, dialect) => post('/api/ai/asr/recognize', { audioUrl, dialect }),
    /** ASR 跟读匹配 */
    asrMatch: (audioUrl, expectedText, dialect) =>
      post('/api/ai/asr/match', { audioUrl, expectedText, dialect }),
    /** ASR 降级策略 */
    asrFallback: (reason) => get('/api/ai/asr/fallback' + buildQuery({ reason })),
    /** ASR 方言列表 */
    dialects: () => get('/api/ai/asr/dialects'),
    /** AI 图片生成（角色头像） */
    imageGenerate: (prompt) => post('/api/ai/image/generate', { prompt }),
  };

  // ============================================================
  // 声音克隆 API（VoiceClone，家长录制 → 训练 → 选用）
  // ============================================================
  const voiceClone = {
    /** 提交克隆任务 */
    submit: (voiceName, sampleAudioUrl) =>
      post('/api/ai/voice/clone', { voiceName, sampleAudioUrl }),
    /** 列出当前家庭克隆音色 */
    list: () => get('/api/ai/voice/clone/list'),
    /** 查询单条状态 */
    get: (id) => get('/api/ai/voice/clone/' + id),
    /** 软删除 */
    remove: (id) => del('/api/ai/voice/clone/' + id),
  };

  // ============================================================
  // 远程共读 API（二期 M07/M34，HTTP 轮询）
  // ============================================================
  const coread = {
    /** 创建房间 */
    create: (childId, bookId, taskId, mode) =>
      post('/api/coread/create', { childId, bookId, taskId, mode }),
    /** 加入房间 */
    join: (roomCode, role) => post('/api/coread/join', { roomCode, role }),
    /** 退出房间 */
    exit: (roomCode, role) => post('/api/coread/' + roomCode + '/exit', { role }),
    /** 关闭房间 */
    close: (roomCode) => post('/api/coread/' + roomCode + '/close', {}),
    /** 查询房间状态 */
    get: (roomCode) => get('/api/coread/' + roomCode),
    /** 同步状态（翻页/暂停/继续） */
    sync: (roomCode, eventType, eventData, senderRole) =>
      post('/api/coread/' + roomCode + '/sync', { eventType, eventData, senderRole }),
    /** 拉取事件（HTTP 轮询） */
    events: (roomCode, sinceId) =>
      get('/api/coread/' + roomCode + '/events' + buildQuery({ sinceId })),
    /** 家长语音插入 */
    voiceInsert: (roomCode, audioUrl, text) =>
      post('/api/coread/' + roomCode + '/voice-insert', { audioUrl, text }),
    /** 切换参与模式 */
    switchMode: (roomCode, mode) =>
      post('/api/coread/' + roomCode + '/switch-mode', { mode }),
    /** 我发起的房间 */
    my: () => get('/api/coread/my'),
    /** 清理过期房间 */
    cleanExpired: () => post('/api/coread/clean-expired', {}),
  };

  // ============================================================
  // 订单与会员 API（M05 会员升级 / 订单状态）
  // ============================================================
  const order = {
    /** 当前家庭会员信息（含 memberLevel / memberEndAt / 剩余天数） */
    memberInfo: () => get('/api/user/member'),
    /** 开通 7 天试用（路由：POST /api/user/trial） */
    startTrial: () => post('/api/user/trial'),
    /** 创建订单（待支付） body: {planType: monthly|quarterly|yearly, paymentMethod: alipay|wxpay|manual} */
    create: (planType, paymentMethod) => post('/api/order', { planType, paymentMethod }),
    /** 支付订单（mock 网关，直接置为已支付） */
    pay: (orderId, paymentMethod) => post('/api/order/' + orderId + '/pay', { paymentMethod }),
    /** 一键下单+支付 */
    renew: (planType, paymentMethod) => post('/api/order/renew', { planType, paymentMethod }),
    /** 订单列表 */
    list: () => get('/api/order/list'),
    /** 订单详情 */
    detail: (orderId) => get('/api/order/' + orderId),
    /** 申请退款 */
    refund: (orderId, reason) => post('/api/order/' + orderId + '/refund', { reason }),
  };

  // ============================================================
  // 工具函数
  // ============================================================

  /**
   * 构建查询字符串
   */
  function buildQuery(params) {
    if (!params) return '';
    const parts = [];
    Object.keys(params).forEach(key => {
      const val = params[key];
      if (val !== undefined && val !== null && val !== '') {
        parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(val));
      }
    });
    return parts.length > 0 ? '?' + parts.join('&') : '';
  }

  /**
   * 获取 URL 参数
   */
  function getQueryParam(name) {
    const params = new URLSearchParams(window.location.search);
    return params.get(name);
  }

  /**
   * Toast 提示（轻量实现）
   */
  function toast(msg, duration) {
    duration = duration || 2000;
    const el = document.createElement('div');
    el.style.cssText = 'position:fixed;top:20px;left:50%;transform:translateX(-50%);background:#24312e;color:#fff;padding:10px 20px;border-radius:8px;z-index:9999;font-size:14px;box-shadow:0 4px 12px rgba(0,0,0,.2);';
    el.textContent = msg;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), duration);
  }

  /**
   * 兼容旧调用形式 apiRequest(url, options)
   * options: { method?: 'GET'|'POST'|'PUT'|'DELETE', body?: string, headers?: object }
   * 返回解析后的 JSON 对象 { code, msg, data }
   */
  async function apiRequestLegacy(url, options) {
    options = options || {};
    const method = (options.method || 'GET').toUpperCase();
    const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
    const token = getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const cfg = { method: method, headers: headers };
    if (options.body && method !== 'GET' && method !== 'HEAD') cfg.body = options.body;
    try {
      const response = await fetch(BASE_URL + url, cfg);
      const contentType = response.headers.get('content-type') || '';
      let result;
      if (contentType.includes('application/json')) {
        result = await response.json();
      } else {
        result = { code: response.ok ? 200 : 500, msg: response.statusText, data: await response.text() };
      }
      if (response.status === 401 || response.status === 403) {
        clearAuth();
        console.warn('认证失效:', url);
      }
      return result;
    } catch (e) {
      console.error('API请求失败:', url, e);
      return { code: 500, msg: '网络异常，请稍后重试', data: null };
    }
  }

  // ============================================================
  // 导出
  // ============================================================
  const api = {
    // Token
    getToken, setToken, getUser, setUser, clearAuth, isLoggedIn, isAdmin,
    // 请求
    get, post, put, del, upload,
    apiRequest: apiRequestLegacy, // 兼容旧代码: apiRequest(path, { method, body })
    // API 分组
    auth, home, book, reading, story, chat, file,
    adminStats, adminConfig, adminAiModel,
    habit, recommend, ai, voiceClone, coread, order,
    // 工具
    getQueryParam, toast,
    // 配置
    BASE_URL,
  };

  // 暴露全局别名（兼容旧代码中直接调用 TokenUtil/apiRequest/toast/BookApi 等）
  if (typeof window !== 'undefined') {
    window.TokenUtil = {
      get: getToken, set: setToken,
      getUser, setUser, clearAuth, isLoggedIn, isAdmin,
      setUserInfo: setUser, // 别名
    };
    window.apiRequest = apiRequestLegacy;
    window.apiGet = get;
    window.apiPost = post;
    window.toast = toast;
    window.BookApi = book;
    window.AuthApi = auth;
    window.API = api;
    window.HabitApi = habit;
    window.RecommendApi = recommend;
    window.AiApi = ai;
    window.CoReadApi = coread;
    window.OrderApi = order;
  }
  return api;
})();
