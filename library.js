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

        form.querySelectorAll('input, select, textarea').forEach(element => {

            element.removeAttribute('onchange');

        });

    });


    // Search functionality

    const searchInput = document.getElementById('search');

    const clearSearchBtn = document.getElementById('clearSearch');

    const bookRows = document.querySelectorAll('.book-row');


    if (searchInput && bookRows.length > 0) {

        searchInput.addEventListener('input', function() {

            const searchTerm = this.value.toLowerCase().trim();

            

            bookRows.forEach(row => {

                if (searchTerm === '') {

                    row.classList.remove('hidden');

                    return;

                }


                // Get text content from relevant cells (title, author, category, publisher)

                const cells = row.querySelectorAll('td');

                const searchableText = [

                    cells[0]?.textContent || '', // Title

                    cells[1]?.textContent || '', // Subtitle

                    cells[2]?.textContent || '', // Authors

                    cells[3]?.textContent || '', // Category

                    cells[4]?.textContent || '', // Publisher

                    cells[6]?.textContent || ''  // Description

                ].join(' ').toLowerCase();


                if (searchableText.includes(searchTerm)) {

                    row.classList.remove('hidden');

                } else {

                    row.classList.add('hidden');

                }

            });


            // Update pagination after search

            updateVisibleRows();

            currentPage = 1;

            displayPage(currentPage);

            setupPagination();

        });


        clearSearchBtn.addEventListener('click', function() {

            searchInput.value = '';

            bookRows.forEach(row => row.classList.remove('hidden'));

            updateVisibleRows();

            currentPage = 1;

            displayPage(currentPage);

            setupPagination();

        });

    }


    // Pagination variables

    const table = document.getElementById('booksTable');

    const tableBody = document.getElementById('tableBody');

    const rows = tableBody.querySelectorAll('tr');

    const paginationDiv = document.getElementById('pagination');

    let sortedRows = Array.from(rows);

    let visibleRows = Array.from(rows);


    const rowsPerPage = 10;

    let currentPage = 1;


    // Update visible rows based on search/filters

    function updateVisibleRows() {

        visibleRows = sortedRows.filter(row => !row.classList.contains('hidden'));

    }


    // Initialize pagination

    function setupPagination() {

        updateVisibleRows();

        const pageCount = Math.ceil(visibleRows.length / rowsPerPage);

        paginationDiv.innerHTML = '';


        if (pageCount <= 1) return;


        // Previous button

        const prevButton = document.createElement('button');

        prevButton.innerText = 'Previous';

        prevButton.disabled = currentPage === 1;

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

        nextButton.disabled = currentPage === pageCount;

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

        updateVisibleRows();

        const start = (page - 1) * rowsPerPage;

        const end = start + rowsPerPage;


        // Hide all rows

        rows.forEach(row => row.style.display = 'none');


        // Show rows for current page (only visible ones)

        for (let i = start; i < end && i < visibleRows.length; i++) {

            visibleRows[i].style.display = '';

        }


        // Update active page button

        setupPagination();

    }


    // Initialize

    if (paginationDiv) {

        setupPagination();

        displayPage(currentPage);

    }

});
