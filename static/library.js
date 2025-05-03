document.addEventListener('DOMContentLoaded', function() {
    const table = document.getElementById('booksTable');
    const tableBody = document.getElementById('tableBody');
    const rows = tableBody.querySelectorAll('tr');
    const paginationDiv = document.getElementById('pagination');
    const originalRows = Array.from(rows);

    // Initialize description toggle functionality
    function setupDescriptionToggle() {
        const showMoreButtons = document.querySelectorAll('.show-more-btn');
        showMoreButtons.forEach(button => {
            button.addEventListener('click', function() {
                const descriptionText = this.previousElementSibling;
                descriptionText.classList.toggle('expanded');
                this.textContent = descriptionText.classList.contains('expanded')
                    ? 'Show less'
                    : 'Show more';
            });
        });
    }

    // Pagination variables
    const rowsPerPage = 10;
    let currentPage = 1;
    let sortedRows = Array.from(rows);

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

    // Sorting functionality
    const headers = table.querySelectorAll('th.sortable');

    headers.forEach(function(header, index) {
        header.addEventListener('click', function() {
            // Determine current sort direction
            const isAscending = header.classList.contains('asc');
            const isDescending = header.classList.contains('desc');

            // Remove all sort classes
            headers.forEach(th => {
                th.classList.remove('asc', 'desc');
            });

            // Set new sort direction
            if (!isAscending && !isDescending) {
                header.classList.add('asc');
                sortTable(index, 'asc');
            } else if (isAscending) {
                header.classList.add('desc');
                sortTable(index, 'desc');
            } else {
                // If it was descending, reset to original order
                resetTableOrder();
            }

            // Reset to first page after sorting
            currentPage = 1;
            displayPage(currentPage);
            setupPagination();
        });
    });

    function sortTable(columnIndex, direction) {
        sortedRows.sort(function(rowA, rowB) {
            const cellA = rowA.cells[columnIndex].textContent.trim();
            const cellB = rowB.cells[columnIndex].textContent.trim();

            // For dates
            if (columnIndex === 5) {
                const dateA = new Date(cellA);
                const dateB = new Date(cellB);
                return direction === 'asc' ? dateA - dateB : dateB - dateA;
            }

            // For numbers (page count, rating)
            if (columnIndex === 7 || columnIndex === 8) {
                const numA = parseFloat(cellA) || 0;
                const numB = parseFloat(cellB) || 0;
                return direction === 'asc' ? numA - numB : numB - numA;
            }

            // Default string comparison
            return direction === 'asc'
                ? cellA.localeCompare(cellB)
                : cellB.localeCompare(cellA);
        });

        // Re-render sorted rows
        rows.forEach(row => row.remove());
        sortedRows.forEach(row => tableBody.appendChild(row));
    }

    function resetTableOrder() {
        sortedRows = Array.from(originalRows);
        rows.forEach(row => row.remove());
        sortedRows.forEach(row => tableBody.appendChild(row));
    }

    // Initialize
    setupDescriptionToggle();
    setupPagination();
    displayPage(currentPage);
});
// Add to library.js

// Make tracking forms submit on all changes
document.querySelectorAll('.tracking-form input, .tracking-form select, .tracking-form textarea').forEach(element => {
    element.addEventListener('change', function() {
        this.form.submit();
    });
});

// Toggle visibility of tracking sections based on status
function updateTrackingSections() {
    document.querySelectorAll('.tracking-form').forEach(form => {
        const statusSelect = form.querySelector('select[name="status"]');
        const readingSections = form.querySelectorAll('.progress-section, .dates-section, .rating-section, .notes-section');
        const readSections = form.querySelectorAll('.dates-section, .rating-section, .notes-section');

        if (statusSelect.value === 'reading') {
            readingSections.forEach(section => section.style.display = 'block');
        } else if (statusSelect.value === 'read') {
            readSections.forEach(section => section.style.display = 'block');
        } else {
            readingSections.forEach(section => section.style.display = 'none');
        }
    });
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // ... existing code ...

    updateTrackingSections();
});