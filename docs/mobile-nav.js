/* ============================================================
   小芽绘本 - 移动端底部导航栏
   在竖屏（<1024px）时自动显示，桌面端不显示
   不改任何 HTML 结构，纯 JS 注入
   ============================================================ */
(function () {
  var BREAKPOINT = 1024;
  var navId = 'mobile-bottom-nav';

  var navItems = [
    { href: 'home.html', icon: 'lucide:home', label: '首页' },
    { href: 'library.html', icon: 'lucide:book-open', label: '绘本库' },
    { href: 'studio.html', icon: 'lucide:sparkles', label: '故事营' },
    { href: 'reader.html', icon: 'lucide:headphones', label: '伴读' },
    { href: 'parent.html', icon: 'lucide:user', label: '家长' }
  ];

  function getCurrentPage() {
    var path = window.location.pathname;
    var file = path.substring(path.lastIndexOf('/') + 1) || 'home.html';
    return file;
  }

  function createNav() {
    var existing = document.getElementById(navId);
    if (existing) return;

    var nav = document.createElement('nav');
    nav.id = navId;
    nav.innerHTML = `
      <style>
        #${navId} {
          position: fixed;
          bottom: 0;
          left: 0;
          right: 0;
          display: flex;
          justify-content: space-around;
          align-items: center;
          background: rgba(255, 255, 255, 0.95);
          backdrop-filter: blur(10px);
          -webkit-backdrop-filter: blur(10px);
          box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);
          border-top: 1px solid rgba(0, 0, 0, 0.06);
          z-index: 9999;
          padding: 6px 0 env(safe-area-inset-bottom, 6px);
          transition: transform 0.3s ease;
        }
        #${navId} .mn-item {
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 2px;
          padding: 6px 10px;
          border: none;
          background: none;
          cursor: pointer;
          text-decoration: none;
          color: #94a3b8;
          font-size: 11px;
          font-weight: 500;
          transition: all 0.2s ease;
          border-radius: 12px;
          min-width: 56px;
        }
        #${navId} .mn-item iconify-icon {
          font-size: 22px;
          width: 22px;
          height: 22px;
          transition: transform 0.2s ease;
        }
        #${navId} .mn-item.active {
          color: #ef765e;
        }
        #${navId} .mn-item.active iconify-icon {
          transform: scale(1.15) translateY(-2px);
        }
        #${navId} .mn-item:hover {
          color: #ef765e;
        }
        #${navId} .mn-item:hover iconify-icon {
          transform: scale(1.1);
        }
        body { padding-bottom: 60px !important; }
        @media (min-width: 1024px) {
          #${navId} { display: none !important; }
          body { padding-bottom: 0 !important; }
        }
      </style>
    `;

    var currentPage = getCurrentPage();
    navItems.forEach(function (item) {
      var a = document.createElement('a');
      a.className = 'mn-item' + (item.href === currentPage ? ' active' : '');
      a.href = item.href;
      a.innerHTML = '<iconify-icon icon="' + item.icon + '" width="22"></iconify-icon><span>' + item.label + '</span>';
      nav.appendChild(a);
    });

    document.body.appendChild(nav);
  }

  function removeNav() {
    var nav = document.getElementById(navId);
    if (nav) nav.remove();
  }

  function update() {
    if (window.innerWidth < BREAKPOINT) {
      createNav();
    } else {
      removeNav();
    }
  }

  // 首次执行
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', update);
  } else {
    update();
  }

  // 监听窗口变化
  var resizeTimer;
  window.addEventListener('resize', function () {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(update, 150);
  });
})();
