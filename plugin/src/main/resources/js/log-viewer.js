AJS.toInit(function() {
    AJS.$("#refresh-btn").click(function() {
        alert("Refreshing logs... (demo)");
        // 실제 구현에서는 AJAX 호출로 서블릿에서 최신 로그를 가져오도록 작성
    });
});
