document.addEventListener('DOMContentLoaded', function() {
    // Description toggle functionality
    document.querySelectorAll('.show-more-btn').forEach(button => {
        button.addEventListener('click', function() {
            const description = this.previousElementSibling;
            description.classList.toggle('expanded');
            this.textContent = description.classList.contains('expanded')
                ? 'Show less'
                : 'Show more';
        });
    });

    // Form submission handlers
    document.querySelectorAll('.tracking-form').forEach(form => {
        // Remove individual onchange handlers since we'll handle submission with the button
        form.querySelectorAll('input, select, textarea').forEach(element => {
            element.removeAttribute('onchange');
        });
    });

    // Rest of your existing JavaScript (pagination, sorting, etc.)
    const table = document.getElementById('booksTable');
    const tableBody = document.getElementById('tableBody');
    const rows = tableBody.querySelectorAll('tr');
    const paginationDiv = document.getElementById('pagination');
    const originalRows = Array.from(rows);
    let sortedRows = Array.from(rows);

    // Pagination variables
    const rowsPerPage = 10;
    let currentPage = 1;

    // Initialize pagination
    function setupPagination() {
        const pageCount = Math.ceil(sortedRows.length / rowsPerPage);
        paginationDiv.innerHTML = '';

        if (pageCount <= 1) return;

        // Previous button
        const prevButton = document.createElement('button');
        prevButton.innerText = 'Previous';
        prevButton.addEventListener('click', function() {
            if (currentPage > 1) {
                currentPage--;
                displayPage(currentPage);
            }
        });
        paginationDiv.appendChild(prevButton);

        // Page buttons
        for (let i = 1; i <= pageCount; i++) {
            const pageButton = document.createElement('button');
            pageButton.innerText = i;
            if (i === currentPage) {
                pageButton.classList.add('active');
            }
            pageButton.addEventListener('click', function() {
                currentPage = i;
                displayPage(currentPage);
            });
            paginationDiv.appendChild(pageButton);
        }

        // Next button
        const nextButton = document.createElement('button');
        nextButton.innerText = 'Next';
        nextButton.addEventListener('click', function() {
            if (currentPage < pageCount) {
                currentPage++;
                displayPage(currentPage);
            }
        });
        paginationDiv.appendChild(nextButton);
    }

    // Display a specific page
    function displayPage(page) {
        const start = (page - 1) * rowsPerPage;
        const end = start + rowsPerPage;

        // Hide all rows
        rows.forEach(row => row.style.display = 'none');

        // Show rows for current page
        for (let i = start; i < end && i < sortedRows.length; i++) {
            sortedRows[i].style.display = '';
        }

        // Update active page button
        const buttons = paginationDiv.querySelectorAll('button');
        buttons.forEach(button => {
            button.classList.remove('active');
            if (button.innerText === page.toString()) {
                button.classList.add('active');
            }
        });
    }

    // Initialize
    setupPagination();
    displayPage(currentPage);
});