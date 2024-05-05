from setuptools import find_packages, setup

package_name = 'l_chika'

setup(
    name=package_name,
    version='0.0.0',
    packages=find_packages(exclude=['test']),
    data_files=[
        ('share/ament_index/resource_index/packages',
            ['resource/' + package_name]),
        ('share/' + package_name, ['package.xml']),
    ],
    install_requires=['setuptools'],
    zip_safe=True,
    maintainer='tetsuya',
    maintainer_email='horie-t@users.noreply.github.com',
    description='L-Chika',
    license='Apache-2.0',
    tests_require=['pytest'],
    entry_points={
        'console_scripts': [
            'l_chika_node = l_chika.l_chika_node:main'
        ],
    },
)
