function toggleRootCats(open) {
    const panel = document.getElementById('rootCats');
    const trigger = document.getElementById('catTrigger');
    if (!panel || !trigger) return;

    panel.classList.toggle('is-open', open);
    trigger.setAttribute('aria-expanded', open ? 'true' : 'false');
}

document.addEventListener('DOMContentLoaded', () => {
    const trigger = document.getElementById('catTrigger');
    const panel = document.getElementById('rootCats');
    if (!trigger || !panel) return;

    // 모바일에서는 클릭 토글
    trigger.addEventListener('click', (e) => {
        if (window.matchMedia('(max-width: 768px)').matches) {
            e.preventDefault();
            panel.classList.toggle('is-open');
        }
    });

    // 모바일 바깥 클릭 시 닫기
    document.addEventListener('click', (e) => {
        if (!window.matchMedia('(max-width: 768px)').matches) return;

        const box = trigger.closest('.sub-item--dropdown');
        if (box && !box.contains(e.target)) {
            panel.classList.remove('is-open');
        }
    });
});