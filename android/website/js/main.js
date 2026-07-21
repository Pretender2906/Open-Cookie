document.addEventListener('DOMContentLoaded', () => {
    setTimeout(() => {
        document.body.classList.remove('no-transition');
    }, 100);

    const yearSpan = document.getElementById('current-year');
    if (yearSpan) {
        yearSpan.textContent = new Date().getFullYear();
    }

    document.querySelectorAll('[data-copy]').forEach((button) => {
        button.addEventListener('click', async () => {
            const selector = button.getAttribute('data-copy');
            const target = selector ? document.querySelector(selector) : null;
            if (!target) return;

            const text = ('value' in target && target.value)
                ? target.value.trim()
                : target.textContent.trim();
            const feedback = button.closest('.contact-block')?.querySelector('.copy-feedback')
                || document.getElementById('copy-feedback');

            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                } else {
                    const range = document.createRange();
                    range.selectNodeContents(target);
                    const selection = window.getSelection();
                    selection.removeAllRanges();
                    selection.addRange(range);
                    document.execCommand('copy');
                    selection.removeAllRanges();
                }
                if (feedback) {
                    feedback.textContent = 'Copied to clipboard.';
                }
            } catch {
                if (feedback) {
                    feedback.textContent = 'Select the address above and copy manually.';
                }
            }
        });
    });

    const revealElements = document.querySelectorAll('.reveal');

    const revealOnScroll = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            }
        });
    }, {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    });

    revealElements.forEach(el => revealOnScroll.observe(el));

    const cookie = document.querySelector('.cookie-wrapper');
    if (cookie) {
        window.addEventListener('mousemove', (e) => {
            const x = (window.innerWidth / 2 - e.pageX) / 50;
            const y = (window.innerHeight / 2 - e.pageY) / 50;
            cookie.style.transform = `translate(${x}px, ${y}px)`;
        });
    }
});
