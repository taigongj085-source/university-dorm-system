/* 宿舍管理系统 · 静态演示交互 */
(function () {
  document.querySelectorAll('[data-confirm]').forEach(function (el) {
    el.addEventListener('click', function (e) {
      if (!window.confirm(el.getAttribute('data-confirm'))) {
        e.preventDefault();
      }
    });
  });

  document.querySelectorAll('form[data-demo]').forEach(function (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var msg = form.getAttribute('data-success') || '提交成功（静态演示）';
      alert(msg);
      var jump = form.getAttribute('data-jump');
      if (jump) window.location.href = jump;
    });
  });
})();
